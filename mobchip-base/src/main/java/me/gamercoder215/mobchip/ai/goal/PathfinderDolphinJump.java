package me.gamercoder215.mobchip.ai.goal;

import org.bukkit.entity.Dolphin;
import org.jetbrains.annotations.NotNull;

import net.minecraft.world.entity.ai.goal.DolphinJumpGoal;

/**
 * Represets a Pathfinder that makes Dolphins Jump
 */
public final class PathfinderDolphinJump extends Pathfinder {

    private int interval;

    /**
     * Constructs a PathfinderDolphinJump from a NMS DolphinJumpGoal.
     * @param g Goal to use
     */
    public PathfinderDolphinJump(@NotNull DolphinJumpGoal g) {
        super(Pathfinder.getEntity(g, "b"));

        this.interval = Pathfinder.getInt(g, "c");
    }

    /**
     * Constructs a PathfinderDolphinJump.
     * @param d Dolphin to use
     * @param interval Interval to use
     * @throws IllegalArgumentException if less than 0
     */
    public PathfinderDolphinJump(@NotNull Dolphin d, int interval) throws IllegalArgumentException {
        super(d);

        if (interval < 0) throw new IllegalArgumentException("Interval must be greater than 0");
        this.interval = interval;
    }

    /**
     * Get the current interval
     * @return
     */
    public int getInterval() {
        return this.interval;
    }

    
    /**
     * Sets the Interval of how often Dolphins will jump.
     * @param interval Interval to set
     * @throws IllegalArgumentException if interval is less than 0
     */
    public void setInterval(int interval) throws IllegalArgumentException {
        if (interval < 0) throw new IllegalArgumentException("Must be greater than 0");
        this.interval = interval;
    }

    @Override
    public DolphinJumpGoal getHandle() {
        return new DolphinJumpGoal((net.minecraft.world.entity.animal.Dolphin) nmsEntity, interval);
    }
    
}