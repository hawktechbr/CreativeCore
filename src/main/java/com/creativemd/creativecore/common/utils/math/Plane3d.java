package com.creativemd.creativecore.common.utils.math;

import java.util.Vector;

import javax.annotation.Nullable;
import javax.vecmath.Matrix3d;
import javax.vecmath.Matrix4d;
import javax.vecmath.Vector3d;

import com.creativemd.creativecore.common.collision.CreativeAxisAlignedBB;
import com.creativemd.creativecore.common.utils.math.BoxUtils.BoxCorner;
import com.creativemd.creativecore.common.utils.math.MatrixUtils.MatrixLookupTable;

import net.minecraft.util.EnumFacing.Axis;
import net.minecraft.util.math.AxisAlignedBB;

public class Plane3d {
	
	public final CreativeAxisAlignedBB bb;
	public final PlaneCache cache;
	protected final Vector3d origin;
	protected final Vector3d normal;
	
	public Plane3d(CreativeAxisAlignedBB bb, PlaneCache cache, Vector3d[] corners, BoxCorner[] planeCorners)
	{
		this.bb = bb;
		this.cache = cache;
		this.origin = corners[planeCorners[0].ordinal()];
		Vector3d first = corners[planeCorners[1].ordinal()];
		Vector3d second = corners[planeCorners[2].ordinal()];
		
		first.sub(origin);
		second.sub(origin);
		this.normal = new Vector3d(first.y * second.z - first.z * second.y, first.z * second.x - first.x * second.z, first.x * second.y - first.y * second.x);
	}
	
	public Boolean isInFront(Vector3d vec)
	{
		double scalar = (vec.x - origin.x)*normal.x + (vec.y - origin.y)*normal.y + (vec.z - origin.z)*normal.z;
		if(scalar > 0)
			return true;
		else if(scalar < 0)
			return false;
		return null;
	}
	
	public static final int accuracySteps = 10; 
	
	public Double binarySearch(@Nullable Double value, AxisAlignedBB toCheck, double checkRadiusSquared, Vector3d center, MatrixLookupTable table)
	{
		if(table.isSimple)
		{
			Double t = searchBetweenSimple(value, toCheck, center, new Vector3d(center), new Vector3d(), 0, 1, table, 0);
			if(t != null && intersects(toCheck, checkRadiusSquared, center, t, table))
				return t;
			
			return null;
		}
		else if(table.hasOneRotation && !table.hasTranslation)
		{
			int halfRotations = table.getNumberOfHalfRotations();
			double halfRotationSize = 1D / halfRotations;
			
			Vector3d temp = new Vector3d();
			Vector3d start = new Vector3d(center);
			
			Double t = searchBetweenSimple(value, toCheck, center, start, temp, 0, halfRotationSize, table, 0);
			if(t != null && intersects(toCheck, checkRadiusSquared, center, t, table))
				return t;
			
			start.set(center);
			table.transformInverted(start, halfRotationSize);
			t = searchBetweenSimple(value, toCheck, center, new Vector3d(center), temp, halfRotationSize, halfRotationSize*2, table, 0);
			if(t != null && intersects(toCheck, checkRadiusSquared, center, t, table))
				return t;
			
			return null;
		}
		
		// Advanced!!!!!!! At this point there is no way to figure out how the matrix behaves, so just scan somewhere with the given resolution and hope to find the earliest hit
		Vector3d start = new Vector3d(center);
		Vector3d temp = new Vector3d();
		int halfRotations = table.getNumberOfHalfRotations();
		double halfRotationSize = 1D / halfRotations;
		for (int i = 0; i < halfRotations; i++) {
			double startT = halfRotationSize * i;
			double endT = halfRotationSize * (i+1);
			
			if(startT != 0)
			{
				start.set(center);
				table.transformInverted(start, startT);
			}
			
			if(value <= startT)
				return null;
			
			Double t = searchBetweenSimple(value, toCheck, center, start, temp, startT, endT, table, 0);
			if(t != null && intersects(toCheck, checkRadiusSquared, center, t, table))
				return t;
		}
		
		return null;
	}
	
	protected Double searchBetweenSimple(@Nullable Double value, AxisAlignedBB toCheck, Vector3d center, Vector3d start, Vector3d temp, double startT, double endT, MatrixLookupTable table, int steps)
	{
		if(value != null && value < startT)
			return null;
		
		Boolean beforeFront = isInFront(start);
		if(beforeFront == null)
			return startT;
		
		temp.set(center);
		table.transformInverted(temp, endT);
		Boolean afterFront = isInFront(temp);
		if(afterFront == null)
			return endT;
		
		if(beforeFront != afterFront)
		{
			// TODO add auto-accuracy system. depending on tocheck size and how far away it is
			if(steps < accuracySteps)
			{
				steps++;
				double halfT = (startT+endT)/2D;
				
				temp.set(center);
				table.transformInverted(temp, halfT);
				
				Boolean halfFront = isInFront(temp);
				if(halfFront == null)
					return halfT;
				
				if(beforeFront != halfFront)
					return searchBetweenSimple(value, toCheck, center, start, temp, startT, halfT, table, steps);
				return searchBetweenSimple(value, toCheck, center, temp, start, halfT, endT, table, steps);
			}
			return startT;
		}
		return null;
	}
	
	public boolean intersects(AxisAlignedBB toCheck, double checkRadiusSquared, Vector3d center, double t, MatrixLookupTable table)
	{
		if(bb.contains(center))
			return true;
		
		Vector3d temp = new Vector3d(center);
		temp.sub(cache.center);
		if(temp.lengthSquared() >= checkRadiusSquared + cache.radiusSquared)
			return false;
		
		Matrix4d matrix = table.getInverted(t);
		
		double minX = Double.MAX_VALUE;
		double minY = Double.MAX_VALUE;
		double minZ = Double.MAX_VALUE;
		double maxX = -Double.MAX_VALUE;
		double maxY = -Double.MAX_VALUE;
		double maxZ = -Double.MAX_VALUE;
		
		for (int i = 0; i < BoxCorner.values().length; i++) {
			Vector3d corner = BoxCorner.values()[i].getVector(toCheck);
			
			corner.sub(table.origin.translation());
			corner.sub(table.rotationCenter);
			table.origin.rotationInv().transform(corner);
			corner.add(table.rotationCenter);
			
			table.transform(matrix, table.rotationCenter, corner);
			
			if(bb.contains(corner))
				return true;
			
			minX = Math.min(minX, corner.x);
			minY = Math.min(minY, corner.y);
			minZ = Math.min(minZ, corner.z);
			maxX = Math.max(maxX, corner.x);
			maxY = Math.max(maxY, corner.y);
			maxZ = Math.max(maxZ, corner.z);
		}
		
		return bb.intersects(minX, minY, minZ, maxX, maxY, maxZ);
	}
	
	public static Plane3d[] getPlanes(CreativeAxisAlignedBB box, PlaneCache cache, MatrixLookupTable table)
	{
		Vector3d[] corners = BoxUtils.getCorners(box);
		
		boolean needsX = table.hasRotX;
		boolean needsY = table.hasRotY;
		boolean needsZ = table.hasRotZ;
		
		if(table.hasX && !needsY && !needsZ)
			needsY = true;
		
		if(table.hasY && !needsX && !needsZ)
			needsX = true;
		
		if(table.hasZ && !needsX && !needsY)
			needsX = true;
		
		Plane3d[] planes = new Plane3d[(needsX ? 2 : 0) + (needsY ? 2 : 0) + (needsZ ? 2 : 0)];
		int index = 0;
		
		if(needsX)
		{
			setPlane(corners, planes, box, cache, Axis.X, index);
			index += 2;
		}
		
		if(needsY)
		{
			setPlane(corners, planes, box, cache, Axis.Y, index);
			index += 2;
		}
		
		if(needsZ)
		{
			setPlane(corners, planes, box, cache, Axis.Z, index);
			index += 2;
		}
		
		return planes;
	}
	
	private static BoxCorner[][] planesForAxis = new BoxCorner[][] {
		{ BoxCorner.EUS, BoxCorner.EDN, BoxCorner.WUS, BoxCorner.WDN },
		{ BoxCorner.EDS, BoxCorner.EUN, BoxCorner.WDS, BoxCorner.WUN },
		
		{ BoxCorner.EUS, BoxCorner.WUN, BoxCorner.EDS, BoxCorner.WDN },
		{ BoxCorner.EUN, BoxCorner.WUS, BoxCorner.EDN, BoxCorner.WDS },
		
		{ BoxCorner.EUS, BoxCorner.WDS, BoxCorner.EUN, BoxCorner.WDN },
		{ BoxCorner.WUS, BoxCorner.EDS, BoxCorner.WUN, BoxCorner.EDN }
	};
	
	private static void setPlane(Vector3d[] corners, Plane3d[] planes, CreativeAxisAlignedBB box, PlaneCache cache, Axis axis, int index)
	{		
		planes[index] = new Plane3d(box, cache, corners, planesForAxis[axis.ordinal() * 2]);
		planes[index + 1] = new Plane3d(box, cache, corners, planesForAxis[axis.ordinal() * 2 + 1]);
	}
	
	public static class PlaneCache {
		
		public Plane3d[] planes;
		public final Vector3d center;
		public final double radiusSquared;
		
		public PlaneCache(AxisAlignedBB box)
		{
			this.radiusSquared = (box.minX * box.maxX + box.minY * box.maxY + box.minZ * box.maxZ) * 0.5;
			this.center = new Vector3d(box.minX + (box.maxX - box.minX) * 0.5D, box.minY + (box.maxY - box.minY) * 0.5D, box.minZ + (box.maxZ - box.minZ) * 0.5D);
		}
		
		public boolean isCached()
		{
			return planes != null;
		}
		
		public void reset()
		{
			planes = null;
		}
		
	}
}
