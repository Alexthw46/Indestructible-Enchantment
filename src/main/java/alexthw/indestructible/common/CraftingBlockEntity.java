package alexthw.indestructible.common;

import alexthw.indestructible.Config;
import alexthw.indestructible.init.Registry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public class CraftingBlockEntity extends BlockEntity {
    public ItemStack stack = ItemStack.EMPTY;

    public CraftingBlockEntity(BlockPos pWorldPosition, BlockState pBlockState) {
        super(Registry.CRAFTING_BLOCK.get(), pWorldPosition, pBlockState);
    }

    public void onDestroyed(BlockState state, BlockPos pos) {
        if (!stack.isEmpty() && level != null) Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
    }

    public InteractionResult onActivated(BlockState state, BlockPos pos, Player player, InteractionHand hand) {
        if (hand == InteractionHand.MAIN_HAND && level != null) {

            ItemStack item = player.getItemInHand(hand);

            if (item.getItem() == Items.BLAZE_POWDER && !stack.isEmpty()){
                item.shrink(1);
                if (stack.getItem() == Items.BOOK){
                    ItemStack stack2 = new ItemStack(Items.ENCHANTED_BOOK);
                    CompoundTag compoundtag = stack.getOrCreateTag();
                    stack2.setTag(compoundtag.copy());
                    stack = stack2;
                }
                stack.enchant(Registry.INDESTRUCTIBLE.get(),1);
                if (Config.COMMON.REPAIR_ENCHANT.get()) stack.setDamageValue(0);
                onDestroyed(state,pos);
                if (!level.isClientSide()) level.sendBlockUpdated(pos, state,state,2); else{
                    level.addParticle(ParticleTypes.EXPLOSION,worldPosition.getX()+0.5, worldPosition.getY()+0.5, worldPosition.getZ()+0.5,0,0,0);
                    this.level.playLocalSound(worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 4.0F, (1.0F + (this.level.random.nextFloat() - this.level.random.nextFloat()) * 0.2F) * 0.7F, false);
                }
                level.removeBlock(pos,false);
                return InteractionResult.SUCCESS;
            }
            else if (item.isEmpty() && !stack.isEmpty()) {
                player.addItem(stack);
                stack = ItemStack.EMPTY;
                if (!level.isClientSide()) level.sendBlockUpdated(pos, state,state,2);
                return InteractionResult.SUCCESS;
            }
            else if (!item.isEmpty() && stack.isEmpty()) {
                if (Registry.INDESTRUCTIBLE.get().canEnchant(item)){
                    stack = item.copy();
                    stack.setCount(1);
                    item.shrink(1);
                    if (item.isEmpty()) player.setItemInHand(hand, ItemStack.EMPTY);
                    if (!level.isClientSide()) level.sendBlockUpdated(pos, state,state,2);
                    return InteractionResult.SUCCESS;
                }
            }
        }
        return InteractionResult.PASS;
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        stack = ItemStack.of(tag.getCompound("stack"));
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        tag = super.save(tag);
        tag.put("stack", stack.save(new CompoundTag()));
        return tag;
    }

    @Override
    public CompoundTag getUpdateTag() {
        return this.save(new CompoundTag());
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt) {
        super.onDataPacket(net, pkt);
        handleUpdateTag(pkt.getTag());
    }

    public BlockPos getWorldPosition(){
        return this.worldPosition;
    }

}
