package io.github.freshglitch.automati;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

// The bucking iron: the ore-bucker's hand tool from real mining history.
// Drop a crushable item on the ground and whack it where it lies — after
// enough blows it shatters into dust. Yields the powered crusher's full
// result half the time, half of it (rounded up) otherwise: better than a
// furnace, humbler than the machine that replaces your arm.
public class BuckingIronItem extends Item {
    public static final int WHACKS_PER_CRUSH = 7;
    private static final String PROGRESS_KEY = "automati_bucking_progress";

    public BuckingIronItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        if (!(level instanceof ServerLevel serverLevel))
            return InteractionResult.SUCCESS; // swing; the server decides what actually happens

        // find a dropped item resting on the clicked block
        BlockPos above = context.getClickedPos().above();
        List<ItemEntity> candidates = serverLevel.getEntitiesOfClass(ItemEntity.class,
            new AABB(above).inflate(0.25, 0.25, 0.25));

        for (ItemEntity target : candidates) {
            SingleRecipeInput input = new SingleRecipeInput(target.getItem());
            var recipe = serverLevel.recipeAccess()
                .getRecipeFor(Automati.CRUSHING_RECIPE_TYPE.get(), input, level);
            if (recipe.isEmpty())
                continue;

            whack(serverLevel, context.getPlayer(), context, target, recipe.get().value().assemble(input));
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    private void whack(ServerLevel level, Player player, UseOnContext context, ItemEntity target, ItemStack result) {
        Vec3 pos = target.position();
        CompoundTag data = target.getPersistentData();
        int progress = data.getIntOr(PROGRESS_KEY, 0) + 1;

        // chips fly off the ore, and the clink rises in pitch as it weakens
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, target.getItem().getItem()),
            pos.x, pos.y + 0.15, pos.z, 6, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.STONE_HIT, SoundSource.BLOCKS,
            1.0F, 0.8F + 0.5F * progress / WHACKS_PER_CRUSH);

        if (progress < WHACKS_PER_CRUSH) {
            data.putInt(PROGRESS_KEY, progress);
            return;
        }

        // the blow that breaks it: consume one item off the stack...
        data.putInt(PROGRESS_KEY, 0);
        ItemStack stack = target.getItem();
        stack.shrink(1);
        if (stack.isEmpty())
            target.discard();
        else
            target.setItem(stack);

        // ...and hand-crushing pays the full crusher result half the time,
        // half rounded up otherwise
        int count = level.getRandom().nextBoolean()
            ? result.getCount()
            : (result.getCount() + 1) / 2;
        ItemEntity product = new ItemEntity(level, pos.x, pos.y + 0.1, pos.z, result.copyWithCount(count));
        product.setDeltaMovement((level.getRandom().nextDouble() - 0.5) * 0.1, 0.2,
            (level.getRandom().nextDouble() - 0.5) * 0.1);
        level.addFreshEntity(product);

        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);

        // one durability per finished crush, not per whack
        if (player != null)
            context.getItemInHand().hurtAndBreak(1, player, context.getHand());
    }
}
