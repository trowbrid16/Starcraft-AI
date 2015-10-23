import java.util.*;

import bwapi.*;
import bwta.BWTA;
import bwta.BaseLocation;

public class TestBot1 extends DefaultBWListener {

    private Mirror mirror = new Mirror();

    private Game game;

    private Player self;
    
    private Unit scout = null;
    private HashSet<Position> enemyBuildingMemory = new HashSet<Position>();
    
    public void run() {
        mirror.getModule().setEventListener(this);
        mirror.startGame();
    }

    @Override
    public void onUnitCreate(Unit unit) {
        System.out.println("New unit " + unit.getType());
    }

    @Override
    public void onStart() {
        game = mirror.getGame();
        self = game.self();

        //Use BWTA to analyze map
        //This may take a few minutes if the map is processed first time!
        System.out.println("Analyzing map...");
        BWTA.readMap();
        BWTA.analyze();
        System.out.println("Map data ready");

    }

    @Override
    public void onFrame() {
        game.setTextSize(10);
        game.drawTextScreen(10, 10, "Playing as " + self.getName() + " - " + self.getRace());
        
        manageUnits();
        manageEnemyBuildings();

    }
    
    /**
     * 
     * @param builder
     * @param buildingType
     * @param aroundTile
     * @return
     */
    public TilePosition getBuildTile(Unit builder, UnitType buildingType, TilePosition aroundTile)
    {
    	TilePosition ret = null;
    	int maxDist = 3;
    	int stopDist = 40;
    	
    	// loop until we find the thing
    	while((maxDist < stopDist) && (ret == null))
    	{
    		// loop through the defined area
    		for(int i = aroundTile.getX()-maxDist; i <= aroundTile.getX()+maxDist; i++)
    		{
    			for(int j = aroundTile.getY()-maxDist; j <= aroundTile.getY()+maxDist; j++)
    			{
    				if(game.canBuildHere(builder, new TilePosition(i,j), buildingType, false))
    				{
    					// move any units that are blocking this tile
    					boolean unitsInWay = false;
    					for(Unit u : game.getAllUnits())
    					{
    						if(u.getID()==builder.getID()) continue;
    						// check if the unit is within 4 of the tile
    						if((Math.abs(u.getTilePosition().getX()-i) < 4) &&
    								(Math.abs(u.getTilePosition().getY()-j) < 4))
    						{
    							unitsInWay = true;
    						}
    					}
    					if(!unitsInWay) return new TilePosition(i,j);
    				}
    			}
    		}
    		// we didn't find a valid tile, so increase max distance
    		maxDist+=2;
    	}
    	
    	if(ret == null) game.printf("Unable to find suitable build position for "+buildingType.toString());
    	return ret;
    }
    
    /**
     * Manage Units
     * 
     */
    public void manageUnits(){
    	StringBuilder units = new StringBuilder("My units:\n");
    	//iterate through my units
        for (Unit myUnit : self.getUnits()) {
            units.append(myUnit.getType()).append(" ").append(myUnit.getTilePosition()).append("\n");

            //if there's enough minerals, and not currently training an SCV, train an SCV
            if (myUnit.getType() == UnitType.Terran_Command_Center && self.minerals() >= 50 && !myUnit.isTraining()) {
                myUnit.train(UnitType.Terran_SCV);
            }

            //if it's a drone and it's idle, decide what to do
            if (myUnit.getType().isWorker() && myUnit.isIdle()) {
            	
            	//Scout
            	if(scout == null && game.enemy().getUnits().size() == 0){
            		scout(myUnit);
            		continue;
            	}
                
            	//if there's enough minerals and we need supply, build supply depot
                if ((self.minerals() >= 100) && (self.supplyTotal()-self.supplyUsed() <= 2)) 
                {
        			// find a place to build a supply depot
        			TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Supply_Depot, self.getStartLocation());
        			// if found, send worker to build it
        			if (buildTile != null)
        			{
        				myUnit.build(buildTile, UnitType.Terran_Supply_Depot);
        			}
                }
                //if there's enough minerals and we don't have too many, build a barracks
                else if(self.minerals() >= 150 && self.allUnitCount(UnitType.Terran_Barracks) + self.incompleteUnitCount(UnitType.Terran_Barracks) <= 2)
                {
                	TilePosition buildTile = getBuildTile(myUnit, UnitType.Terran_Barracks, self.getStartLocation());
                	if(buildTile != null)
                	{
                		myUnit.build(buildTile, UnitType.Terran_Barracks);
                	}
                }
                else
            	{
	            	Unit closestMineral = null;
	            	
	
	                //find the closest mineral
	                for (Unit neutralUnit : game.neutral().getUnits()) {
	                    if (neutralUnit.getType().isMineralField()) {
	                        if (closestMineral == null || myUnit.getDistance(neutralUnit) < myUnit.getDistance(closestMineral)) {
	                            closestMineral = neutralUnit;
	                        }
	                    }
	                }
	            			
	                //if a mineral patch was found, send the drone to gather it
	                if (closestMineral != null) {
	                    myUnit.gather(closestMineral, false);
	                }
            	}
            }
            
            // make the barracks build marines
            if(myUnit.getType() == UnitType.Terran_Barracks && self.minerals() >= 50 && !myUnit.isTraining())
            {
            	myUnit.train(UnitType.Terran_Marine);
            }
        }
        //draw my units on screen
//        game.drawTextScreen(10, 25, units.toString());
    }
    
    public void scout(Unit unit){
    	scout = unit;
    	List<BaseLocation> baseLocations = BWTA.getBaseLocations();
    	for(BaseLocation base : baseLocations){
    		TilePosition baseToP = new TilePosition(base.getX()/32, base.getY()/32);
    		if (base.isStartLocation() && baseToP != self.getStartLocation()){
    			if(unit.isIdle()){
    				unit.move(base.getPosition());
    			}
    			else{
    				unit.move(base.getPosition(), true);
    			}
    		}
    	}
    	//Add home as the last place to go
    	Position homePosition = new Position(self.getStartLocation().getX(), self.getStartLocation().getY());
    	unit.move(homePosition, true);	
    }
    
    public void manageEnemyBuildings(){
    	//Add any buildings we see to list.
    	for(Unit u: game.enemy().getUnits()){
    		//if this unit is a building add it to the hash
    		if(u.getType().isBuilding()){
    			//check if we have it's position in memory and add it if we don't
    			if(!enemyBuildingMemory.contains(u.getPosition())){
    				enemyBuildingMemory.add(u.getPosition());
    			}
    		}
    	}
    	
    	//loop over the visible enemy units that we remember
    	for(Position p : enemyBuildingMemory){
    		TilePosition tileCorrespondingToP = new TilePosition(p.getX()/32, p.getY()/32);
    		
    		//if visible
    		if(game.isVisible(tileCorrespondingToP)){
    			//loop over the visible enemy buildings and find out if at least
    			// one of them is still at the remembered position
    			boolean buildingStillThere = false;
    			for(Unit u: game.enemy().getUnits()){
    				if(u.getType().isBuilding() && u.getPosition() == p){
    					buildingStillThere = true;
    					break;
    				}
    			}
    			if(!buildingStillThere){
    				enemyBuildingMemory.remove(p);
    				break;
    			}
    		}
    	}
    }

    public static void main(String[] args) {
        new TestBot1().run();
    }
}