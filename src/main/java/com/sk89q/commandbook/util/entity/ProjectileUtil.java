package com.sk89q.commandbook.util.entity;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Fireball;
import org.bukkit.util.Vector;

import java.util.HashSet;
import java.util.Set;

public class ProjectileUtil {

    /**
     * Send an arrow from a player eye level.
     *
     * @param loc
     * @param dir
     * @param speed
     */
    public static Arrow sendArrowFromLocation(Location loc,
                                           Vector dir, float speed) {
        Vector actualDir = dir.clone().normalize();
        Vector finalVecLoc = loc.toVector().add(actualDir.multiply(2));
        loc.setX(finalVecLoc.getX());
        loc.setY(finalVecLoc.getY());
        loc.setZ(finalVecLoc.getZ());
        Arrow arrow = loc.getWorld().spawn(loc, Arrow.class);
        arrow.setVelocity(dir.multiply(speed));
        return arrow;
    }

    /**
     * Send fireballs from a player eye level.
     *
     * @param loc
     * @param amt number of fireballs to shoot (evenly spaced)
     */
    public static Set<Fireball> sendFireballsFromLocation(Location loc, int amt) {
        final double tau = 2 * Math.PI;
        double arc = tau / amt;
        Set<Fireball> resultSet = new HashSet<Fireball>();
        for (double a = 0; a < tau; a += arc) {
            Vector dir = new Vector(Math.cos(a), 0, Math.sin(a));
            Location spawn = loc.toVector().add(dir.multiply(2)).toLocation(loc.getWorld(), 0.0F, 0.0F);
            Fireball fball = loc.getWorld().spawn(spawn, Fireball.class);
            fball.setDirection(dir.multiply(10));
            resultSet.add(fball);
        }
        return resultSet;
    }
}
