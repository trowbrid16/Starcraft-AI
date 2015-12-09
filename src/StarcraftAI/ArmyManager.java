package StarcraftAI;
import java.util.ArrayList;
import java.util.List;

import bwapi.Player;
import bwapi.Position;
import bwapi.TilePosition;
import bwta.BWTA;
import bwta.BaseLocation;

/**
 * ArmyManager Class
 * 	This class is responsible for handling all direct orders to
 * 	military units when they are not in combat situations.
 * 
 * @author Casey Sigelmann
 * @author Alex Bowns
 *
 */
public class ArmyManager{
	private Player self;
	private ArrayList<TilePosition> scoutQueue = new ArrayList<TilePosition>();
	private Squad[] squads;
	private TilePosition queuedTile = null;
	private List<BaseLocation> baseLocations = new ArrayList<BaseLocation>(); 

	/**
	 * ctor
	 * 
	 */
	public ArmyManager(Squad[] squads, Player self){
		this.squads = squads;
		this.self = self;
	}

	/**
	 * getBuildingLocations()
	 * Create a list of base locations and add them to scoutQueue
	 */
	public void getBuildingLocations(){
		this.baseLocations = BWTA.getStartLocations();
		
		for(BaseLocation base : this.baseLocations){
			TilePosition baseToP = new TilePosition(base.getX()/32, base.getY()/32);
			if (base.isStartLocation() && baseToP != self.getStartLocation()){
				this.scoutQueue.add(baseToP);
			}			    
		}
	}

	/**
	 * setSquads
	 * given a list of squads, set those squads as the squads 
	 * for this class to act on. 
	 * 
	 * @param squads - a list of squads 
	 */
	public void setSquads(Squad[] squads){
		this.squads = squads;
	}

	/**
	 * defend()
	 * Positions the defend squad in a defensive position around our base.
	 */
	public void defend()
	{

	}

	/**
	 * engage()
	 * Makes the offensive squad attack the given location.
	 * 
	 * @param position - the location to attack
	 */
	public void engage(Position position)
	{
		for(Squad squad : squads){
			if(squad.getSquadType() == SquadType.Offense){
				squad.attackMove(position);
				break;
			}
		}

	}

	/**
	 * scout()
	 * Moves units in the scout squad to unexplored locations.
	 */
	public void scout()
	{	
		// have the scout squad travel to each untraveled base location
		for(Squad squad : squads){
			if(squad.getSquadType() == SquadType.Offense){
				// move to the base until it is explored
				queuedTile = scoutQueue.get(0);
				Position queuedPosition = convertTilePositionToPosition(queuedTile);
				squad.move(queuedPosition);
				// check if the squad has reached the baselocation	
				if (squad.squadPosition(queuedPosition)){
					//remove the queuedTile
					scoutQueue.remove(queuedTile);
				}		
			}
		}
	}

	/**
	 * convertTilePositionToPosition:
	 * Takes a tile position and turns it into a position object
	 * 
	 * @param tilePosition a tile position object
	 * @return the pixel position or the Position object corresponding to 
	 * 		a given tile position
	 */
	private Position convertTilePositionToPosition(TilePosition tilePosition){
		Position position = new Position(tilePosition.getX()*32, tilePosition.getY()*32);
		return position;
	}
}
