package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

// The crusher block: counter-rotating blade rollers lie exposed on top, and
// while they run they are exactly as dangerous as they look.
public class CrusherBlock extends AbstractMachineBlock {
    private static final float BLADE_DAMAGE = 6.0F;   // an iron sword's worth
    private static final EquipmentSlot[] ARMOR_SLOTS = {
        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD };

    public CrusherBlock(Properties properties) {
        super(properties);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    // Running blades bite whoever stands on them, sneaking or not
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel serverLevel && state.getValue(LIT) && entity instanceof LivingEntity victim)
            bite(serverLevel, pos, victim);
        super.stepOn(level, pos, state, entity);
    }

    private static void bite(ServerLevel level, BlockPos pos, LivingEntity victim) {
        // The blades ignore half the victim's armor. The damage type bypasses
        // armor altogether, so the reduction is worked out here against the
        // halved value and armor is not counted twice.
        DamageSource source = new DamageSource(level.registryAccess()
            .lookupOrThrow(Registries.DAMAGE_TYPE).getOrThrow(Automati.CRUSHER_DAMAGE));
        float damage = CombatRules.getDamageAfterAbsorb(victim, BLADE_DAMAGE, source,
            victim.getArmorValue() * 0.5F, (float) victim.getAttributeValue(Attributes.ARMOR_TOUGHNESS));

        // vanilla never wears down armor that was bypassed, so a landed bite does
        if (victim.hurtServer(level, source, damage)) {
            for (EquipmentSlot slot : ARMOR_SLOTS) {
                ItemStack worn = victim.getItemBySlot(slot);
                if (!worn.isEmpty())
                    worn.hurtAndBreak(2, victim, slot);
            }
        }

        // The rollers pull inward: replace the hurt knockback with a drag
        // toward the centre of the block. hurtMarked makes the server send the
        // new velocity to player clients.
        double pullX = pos.getX() + 0.5 - victim.getX();
        double pullZ = pos.getZ() + 0.5 - victim.getZ();
        double distance = Math.sqrt(pullX * pullX + pullZ * pullZ);
        if (distance > 0.05) {
            Vec3 motion = victim.getDeltaMovement();
            victim.setDeltaMovement(
                motion.x * 0.4 + pullX / distance * 0.15,
                Math.min(motion.y, 0.0),
                motion.z * 0.4 + pullZ / distance * 0.15);
            victim.hurtMarked = true;
        }
    }

    // Grinding noise and the dust the rollers kick up
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;
        if (random.nextDouble() < 0.15)
            playAmbience(level, pos, Automati.CRUSHER_LOOP.get(), 0.7F, 0.95F + random.nextFloat() * 0.1F);
        if (random.nextDouble() < 0.3)
            riseFromTop(level, pos, random, ParticleTypes.POOF, 0.3, 1.05, 0.02);
    }
}
