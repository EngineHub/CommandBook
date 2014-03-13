package com.sk89q.commandbook.util.entity;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.Projectile;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ProjectileUtil {

    /**
     * Send a projectile from an entity's eye level.
     *
     * @param loc
     * @param dir
     * @param speed
     */
    public static <T extends Projectile> T sendProjectileFromLocation(Location loc, Vector dir, float speed, Class<T> clazz) {

        loc = loc.clone();

        Vector actualDir = dir.clone().normalize();
        Vector finalVecLoc = loc.toVector().add(actualDir.multiply(2));
        loc.setX(finalVecLoc.getX());
        loc.setY(finalVecLoc.getY());
        loc.setZ(finalVecLoc.getZ());
        T projectile = loc.getWorld().spawn(loc, clazz);
        if (projectile instanceof Fireball) {
            ((Fireball) projectile).setDirection(dir.multiply(speed));
        } else {
            projectile.setVelocity(dir.multiply(speed));
        }
        return projectile;
    }

    /**
     * Send projectiles from an entity's eye level.
     *
     * @param loc
     * @param amt number of fireballs to shoot (evenly spaced)
     */
    public static <T extends Projectile> Set<T> sendProjectilesFromLocation(Location loc, int amt, float speed, Class<T> clazz) {
        final double tau = 2 * Math.PI;
        double arc = tau / amt;
        Set<T> resultSet = new HashSet<T>();
        for (double a = 0; a < tau; a += arc) {
            resultSet.add(sendProjectileFromLocation(loc, new Vector(Math.cos(a), 0, Math.sin(a)), speed, clazz));
        }
        return resultSet;
    }
}
