package me.gamercoder215.mobchip;

import org.bukkit.entity.Player;
import org.bukkit.entity.Pose;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.util.BoundingBox;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents an Entity's Body and Attributes
 */
public interface EntityBody {

    /**
     * Whether this Entity is Left Handed.
     * @return true if left-handed, else false
     */
    boolean isLeftHanded();

    /**
     * Sets this Entity to be left-handed.
     * @param leftHanded true if left-handed, else false
     */
    void setLeftHanded(boolean leftHanded);

    /**
     * Whether this Mob can breathe underwater.
     * @return true if mob can breathe underwater, else false
     */
    boolean canBreatheUnderwater();

    /**
     * Whether this Mob should discard friction.
     * <p>
     * If this is set to true, the entity's movement will not be amplified while naturally traveling.
     * @return true if mob discards friction, else false
     */
    boolean shouldDiscardFriction();

    /**
     * Sets whether this Mob should discard friction. See {@link #shouldDiscardFriction()} for more information on what this actually does.
     * @param discard true if mob discards friction, else false
     * @see #shouldDiscardFriction()
     */
    void setDiscardFriction(boolean discard);

    /**
     * Represents EquipmentSlots consisting of your Main Hand and Off-Hand
     */
    enum InteractionHand {

        /**
         * Represents the Main Hand
         */
        MAIN_HAND,
        /**
         * Represents the Off-Hand
         */
        OFF_HAND

        ;

        InteractionHand() {}

        /**
         * Converts a MobChip InteractionHand to a NMS InteractionHand.
         * @return NMS InteractionHand
         */
        public net.minecraft.world.InteractionHand getHandle() {
           if (this == MAIN_HAND) return net.minecraft.world.InteractionHand.MAIN_HAND;
           return net.minecraft.world.InteractionHand.OFF_HAND;
        }

        /**
         * Converts this InteractionHand to an EquipmentSlot.
         * @return EquipmentSlot Variant
         */
        public EquipmentSlot toEquipmentSlot() {
            if (this == InteractionHand.MAIN_HAND) return EquipmentSlot.HAND;
            return EquipmentSlot.OFF_HAND;
        }

    }

    /**
     * Represents the result of an interaction
     */
    enum InteractionResult {
        /**
         * Represents a successful interaction result.
         */
        SUCCESS,
        /**
         * Represents an interaction result that caused an item to be consumed.
         */
        CONSUME,
        /**
         * Represents an interaction result that caused an item to be partially consumed.
         */
        CONSUME_PARTIAL,
        /**
         * Represents an interaction result that did nothing.
         */
        PASS,
        /**
         * Represents a failed interaction result.
         */
        FAIL
        ;

        InteractionResult() {}

        /**
         * Converts a MobChip InteractionResult to a NMS InteractionResult.
         * @return NMS InteractionResult
         */
        public net.minecraft.world.InteractionResult getHandle() {
            return switch (this) {
                case SUCCESS -> net.minecraft.world.InteractionResult.SUCCESS;
                case CONSUME -> net.minecraft.world.InteractionResult.CONSUME;
                case CONSUME_PARTIAL -> net.minecraft.world.InteractionResult.CONSUME_PARTIAL;
                case FAIL -> net.minecraft.world.InteractionResult.FAIL;
                default -> net.minecraft.world.InteractionResult.PASS;
            };
        }

    }

    /**
     * Makes this Mob interact with a Player.
     * @param p Player to interact with
     * @param hand Hand to use
     * @return Result of interaction
     */
    InteractionResult interact(@NotNull Player p, @Nullable InteractionHand hand);

    /**
     * Swings this Mob's Hand.
     * @param hand Hand to Broadcast
     */
    default void swing(@Nullable InteractionHand hand) {
        if (hand == null) return;
        swing(hand, false);
    }

    /**
     * Swings this Mob's Hand.
     * @param hand Hand to swing
     * @param broadcast Whether to broadcast and send the Animation Packet sent when interacting (defaults to false or just broadcast)
     */
    void swing(@Nullable InteractionHand hand, boolean broadcast);

    /**
     * Whether this Entity is sensitive to water (e.g. enderman)
     * @return true if sensitive to water, else false
     */
    boolean isSensitiveToWater();

    /**
     * Fetches a BoundingBox for the bounds of an Entity's Pose.
     * @param pose Pose to use
     * @return BoundingBox showing Pose Bounds
     */
    @Nullable
    BoundingBox getPoseBounds(@Nullable Pose pose);

    /**
     * Whether this Mob is affected by Potions.
     * @return true if mob is affected, else false
     */
    boolean isAffectedByPotions();

    /**
     * Whether this Mob is blocking.
     * @return true if mob is blocking, else false
     */
    boolean isBlocking();

    /**
     * Fetch the percentage of how much armor is covering this Mob.
     * @return Percentage of Armor Coverage
     */
    float getArmorCoverPercentage();

    /**
     * Makes this Mob use the Item in its Hand.
     * @param hand Hand to use
     */
    void useItem(@Nullable InteractionHand hand);

    /**
     * Whether this Mob is currently using an Item.
     * @return true if mob is using item, else false
     */
    boolean isUsingItem();

    /**
     * Whether this Mob is Immune to Fire/Lava Damage.
     * @return true if Mob is immune to fire, else false
     */
    boolean isFireImmune();

}