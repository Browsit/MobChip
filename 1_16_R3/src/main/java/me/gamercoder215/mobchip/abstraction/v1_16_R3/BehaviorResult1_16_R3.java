package me.gamercoder215.mobchip.abstraction.v1_16_R3;

import me.gamercoder215.mobchip.abstraction.ChipUtil1_16_R3;
import me.gamercoder215.mobchip.ai.behavior.BehaviorResult;
import net.minecraft.server.v1_16_R3.Behavior;
import net.minecraft.server.v1_16_R3.EntityInsentient;
import net.minecraft.server.v1_16_R3.WorldServer;
import org.bukkit.Bukkit;
import org.jetbrains.annotations.NotNull;

@SuppressWarnings({"unchecked", "rawtypes"})
public final class BehaviorResult1_16_R3 extends BehaviorResult {
    private final Behavior b;
    private final EntityInsentient mob;
    private final WorldServer l;

    public BehaviorResult1_16_R3(Behavior b, EntityInsentient mob) {
        this.b = b;
        this.mob = mob;
        this.l = ChipUtil1_16_R3.toNMS(Bukkit.getWorld(mob.world.getWorld().getUID()));

        b.e(l, mob, 0);
    }

    @Override
    public @NotNull Status getStatus() {
        return ChipUtil1_16_R3.fromNMS(b.a());
    }

    @Override
    public void stop() {
        b.g(l, mob, 0);
    }
}
