package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.CombatRules;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

// The crusher block: counter-rotating blade rollers on the top face, visible
// from every side, so no facing — just LIT while the rollers are spinning.
public class CrusherBlock extends Block implements EntityBlock {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public CrusherBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(LIT, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new CrusherBlockEntity(pos, state);
    }

    @Override
    @Nullable
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (level.isClientSide())
            return null;
        return (lvl, pos, st, blockEntity) -> {
            if (blockEntity instanceof CrusherBlockEntity crusher)
                crusher.serverTick(lvl, pos, st);
        };
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
                && level.getBlockEntity(pos) instanceof CrusherBlockEntity crusher) {
            serverPlayer.openMenu(crusher, pos);
        }
        return InteractionResult.SUCCESS;
    }

    // Standing on spinning blade rollers ends the way you'd expect: an
    // iron-sword-grade bite (6.0) that shrugs off half the victim's armor.
    // We do the armor math ourselves against the halved value, then deliver
    // the result through an armor-bypassing damage type so armor isn't
    // counted twice. Sneaking does not help — the blades don't care.
    @Override
    public void stepOn(Level level, BlockPos pos, BlockState state, Entity entity) {
        if (level instanceof ServerLevel serverLevel && state.getValue(LIT)
                && entity instanceof LivingEntity living) {
            Holder<DamageType> type = serverLevel.registryAccess()
                .lookupOrThrow(Registries.DAMAGE_TYPE)
                .getOrThrow(Automati.CRUSHER_DAMAGE);
            DamageSource source = new DamageSource(type);
            float halvedArmor = living.getArmorValue() * 0.5F;
            float toughness = (float) living.getAttributeValue(Attributes.ARMOR_TOUGHNESS);
            float damage = CombatRules.getDamageAfterAbsorb(living, 6.0F, source, halvedArmor, toughness);

            // Because the damage type bypasses armor, vanilla never wears the
            // armor down itself — so each landed bite chews the worn plating too
            if (living.hurtServer(serverLevel, source, damage)) {
                for (EquipmentSlot slot : new EquipmentSlot[] {
                        EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD }) {
                    ItemStack worn = living.getItemBySlot(slot);
                    if (!worn.isEmpty())
                        worn.hurtAndBreak(2, living, slot);
                }
            }

            // The rollers bite inward: instead of knockback flinging the victim
            // clear, drag them toward the centre of the blades. Damped current
            // velocity + inward pull overrides the hurt knockback; hurtMarked
            // forces the server to sync the new velocity to player clients.
            double pullX = pos.getX() + 0.5 - living.getX();
            double pullZ = pos.getZ() + 0.5 - living.getZ();
            double len = Math.sqrt(pullX * pullX + pullZ * pullZ);
            if (len > 0.05) {
                var motion = living.getDeltaMovement();
                living.setDeltaMovement(
                    motion.x * 0.4 + (pullX / len) * 0.15,
                    Math.min(motion.y, 0.0),
                    motion.z * 0.4 + (pullZ / len) * 0.15);
                living.hurtMarked = true;
            }
        }
        super.stepOn(level, pos, state, entity);
    }

    // Grinding ambience and stone dust kicked up by the rollers
    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!state.getValue(LIT))
            return;

        if (random.nextDouble() < 0.15) {
            level.playLocalSound(pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5,
                Automati.CRUSHER_LOOP.get(), SoundSource.BLOCKS,
                0.7F, 0.95F + random.nextFloat() * 0.1F, false);
        }

        if (random.nextDouble() < 0.3) {
            level.addParticle(ParticleTypes.POOF,
                pos.getX() + 0.3 + random.nextDouble() * 0.4,
                pos.getY() + 1.05,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4,
                0.0, 0.02, 0.0);
        }
    }
}
