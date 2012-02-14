package net.slipcor.pvparena.definitions;

import java.util.Random;

import net.slipcor.pvparena.core.Debug;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

/**
 * region class
 * 
 * -
 * 
 * contains region methods and variables for quicker region access
 * 
 * @author slipcor
 * 
 * @version v0.6.0
 * 
 */

public class ArenaRegion {
	private Debug db = new Debug();
	private boolean cuboid;
	protected Vector min;
	protected Vector max;
	protected World world;
	public String name;

	/**
	 * create a PVP Arena region instance
	 * 
	 * @param sName
	 *            the region name
	 * @param lMin
	 *            the region minimum location
	 * @param lMax
	 *            the region maximum location
	 */
	public ArenaRegion(String sName, Location lMin, Location lMax, boolean cube) {
		name = sName;
		cuboid = cube;
		if (cuboid) {
			Location[] sane = sanityCheck(lMin, lMax);
			lMin = sane[0].clone();
			lMax = sane[1].clone();
		}
		min = lMin.toVector();
		max = lMax.toVector();
		world = lMin.getWorld();
	}

	private Location[] sanityCheck(Location lMin, Location lMax) {
		boolean x =  (lMin.getBlockX() > lMax.getBlockX());
		boolean y = (lMin.getBlockY() > lMax.getBlockY());
		boolean z = (lMin.getBlockZ() > lMax.getBlockZ());
		
		if (!(x|y|z))
		{
			return new Location[] {lMin, lMax};
		}
		Location l1;
		Location l2;

		l1 = new Location(lMin.getWorld(),
				x?lMax.getBlockX():lMin.getBlockX(),
				y?lMax.getBlockY():lMin.getBlockY(),
				z?lMax.getBlockZ():lMin.getBlockZ());
		l2 = new Location(lMin.getWorld(),
				x?lMin.getBlockX():lMax.getBlockX(),
				y?lMin.getBlockY():lMax.getBlockY(),
				z?lMin.getBlockZ():lMax.getBlockZ());
		return new Location[] {l1, l2};
	}

	/**
	 * is a location inside the PVP Arena region?
	 * 
	 * @param vec
	 *            the vector to check
	 * @return
	 */
	public boolean contains(Location loc) {
		if (min == null || max == null || loc == null
				|| loc.getWorld() != world)
			return false; // no arena, no container or not in the same world
		Vector vec = loc.toVector();
		
		if (cuboid) {
		
			db.i("checking region " + name + ": "
					+ String.valueOf(vec.isInAABB(min, max)));
			db.i("("+vec.toString()+" isInAABB "+min.toString()+"/"+max.toString()+")");
			return vec.isInAABB(min, max);
		} else {
			return vec.distance(min.getMidpoint(max)) <= min.distance(max);
		}
	}
	
	

	/**
	 * is a location farther away than a given length?
	 * 
	 * @param offset
	 *            the length to check
	 * @param loc
	 *            the location to check
	 * @return true if the location is more than offset blocks away, false
	 *         otherwise
	 */
	public boolean tooFarAway(int offset, Location loc) {
		if (!world.equals(loc.getWorld()))
			return true;

		db.i("checking join range");
		Vector bvdiff = (Vector) min.getMidpoint(max);

		return (offset < bvdiff.distance(loc.toVector()));
	}

	/**
	 * does the region overlap with another given region?
	 * 
	 * @param paRegion
	 *            the other region
	 * @return true if the regions overlap, false otherwise
	 */
	public boolean overlapsWith(ArenaRegion paRegion) {
		if (this.cuboid && paRegion.cuboid) {
			// compare 2 cuboids
			if (min.getX() > paRegion.max.getX()
					|| min.getY() > paRegion.max.getY()
					|| min.getZ() > paRegion.max.getZ()) {
				return false;
			}
			if (paRegion.min.getX() > max.getX()
					|| paRegion.min.getY() > max.getY()
					|| paRegion.min.getZ() > max.getZ()) {
				return false;
			}
	
			return true;
		} else if (!this.cuboid && !paRegion.cuboid) {
			// compare 2 spheres
			Vector thisCenter = this.max.getMidpoint(this.min);
			Vector thatCenter = paRegion.max.getMidpoint(paRegion.min);

			double thisRadius = this.max.distance(min)/2;
			double thatRadius = paRegion.max.distance(paRegion.min)/2;
			
			return thisCenter.distance(thatCenter) < (thisRadius + thatRadius);
			
		} else if (this.cuboid && !paRegion.cuboid) {
			// we are cube and search for intersecting sphere

			Vector thisCenter = this.max.getMidpoint(this.min);
			Vector thatCenter = paRegion.max.getMidpoint(paRegion.min);
			
			if (contains(thatCenter.toLocation(world))) {
				return true; // the sphere is inside!
			}
			
			Vector diff = thatCenter.subtract(thisCenter); // diff is pointing from that to this
			
			return this.contains(diff.normalize().toLocation(world));
		} else {
			return paRegion.overlapsWith(this); // just check the other freaking way round!
		}
	}

	/**
	 * drop an item in a random region position
	 * 
	 * @param item
	 */
	public void dropItemRandom(Material item) {

		db.i("dropping item");
		int diffx = (int) (min.getX() - max.getX());
		int diffy = (int) (min.getY() - max.getY());
		int diffz = (int) (min.getZ() - max.getZ());

		Random r = new Random();

		int posx = diffx == 0 ? min.getBlockX() : (int) ((diffx / Math
				.abs(diffx)) * r.nextInt(Math.abs(diffx)) + max.getX());
		int posy = diffy == 0 ? min.getBlockY() : (int) ((diffx / Math
				.abs(diffy)) * r.nextInt(Math.abs(diffy)) + max.getY());
		int posz = diffz == 0 ? min.getBlockZ() : (int) ((diffx / Math
				.abs(diffz)) * r.nextInt(Math.abs(diffz)) + max.getZ());

		world.dropItem(new Location(world, posx, posy + 1, posz),
				new ItemStack(item, 1));
	}

	/**
	 * restore the region (atm just remove all arrows and items)
	 */
	public void restore() {

		for (Entity e : world.getEntities()) {
			if (((!(e instanceof Item)) && (!(e instanceof Arrow)))
					|| (!contains(e.getLocation())))
				continue;
			e.remove();
		}
	}
}
