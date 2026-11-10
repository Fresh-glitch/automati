package io.github.freshglitch.automati;

import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.SingleRecipeInput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

// The bucking iron, the ore-bucker's hand tool: drop a crushable item on the
// ground and strike it where it lies until it shatters. It pays the crusher's
// full result half the time and half of it (rounded up) otherwise.
public class BuckingIronItem extends Item {
    public static final int WHACKS_PER_CRUSH = 7;
    private static final String PROGRESS_KEY = "automati_bucking_progress";

    public BuckingIronItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        // The client only swings; the server decides what happens
        if (!(context.getLevel() instanceof ServerLevel level))
            return InteractionResult.SUCCESS;

        // The first crushable item resting on the clicked block
        AABB reach = new AABB(context.getClickedPos().above()).inflate(0.25);
        for (ItemEntity target : level.getEntitiesOfClass(ItemEntity.class, reach)) {
            SingleRecipeInput input = new SingleRecipeInput(target.getItem());
            var recipe = level.recipeAccess().getRecipeFor(Automati.CRUSHING_RECIPE_TYPE.get(), input, level);
            if (recipe.isPresent()) {
                whack(level, context, target, recipe.get().value().assemble(input));
                return InteractionResult.SUCCESS;
            }
        }
        return InteractionResult.PASS;
    }

    private static void whack(ServerLevel level, UseOnContext context, ItemEntity target, ItemStack result) {
        Vec3 pos = target.position();
        CompoundTag data = target.getPersistentData();
        int progress = data.getIntOr(PROGRESS_KEY, 0) + 1;
        boolean shattered = progress >= WHACKS_PER_CRUSH;
        data.putInt(PROGRESS_KEY, shattered ? 0 : progress);

        // Chips fly off, and the clink rises in pitch as the ore weakens
        level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, target.getItem().getItem()),
            pos.x, pos.y + 0.15, pos.z, 6, 0.1, 0.1, 0.1, 0.05);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.STONE_HIT, SoundSource.BLOCKS,
            1.0F, 0.8F + 0.5F * progress / WHACKS_PER_CRUSH);
        if (!shattered)
            return;

        ItemStack stack = target.getItem();
        stack.shrink(1);
        if (stack.isEmpty())
            target.discard();
        else
            target.setItem(stack);

        RandomSource random = level.getRandom();
        int count = random.nextBoolean() ? result.getCount() : (result.getCount() + 1) / 2;
        ItemEntity product = new ItemEntity(level, pos.x, pos.y + 0.1, pos.z, result.copyWithCount(count));
        product.setDeltaMovement((random.nextDouble() - 0.5) * 0.1, 0.2, (random.nextDouble() - 0.5) * 0.1);
        level.addFreshEntity(product);
        level.playSound(null, pos.x, pos.y, pos.z, SoundEvents.GRINDSTONE_USE, SoundSource.BLOCKS, 0.8F, 1.2F);

        // Durability is spent per finished crush, not per whack
        Player player = context.getPlayer();
        if (player != null)
            context.getItemInHand().hurtAndBreak(1, player, context.getHand());
    }
}
