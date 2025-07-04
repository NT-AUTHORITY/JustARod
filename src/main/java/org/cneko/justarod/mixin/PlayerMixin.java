package org.cneko.justarod.mixin;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import org.cneko.justarod.entity.*;
import org.cneko.justarod.packet.PowerSyncPayload;
import org.cneko.toneko.common.mod.entities.INeko;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;

import static org.cneko.justarod.JRAttributes.Companion;

@SuppressWarnings({"AddedMixinMembersNamePattern", "ConstantValue", "DataFlowIssue"})
@Mixin(PlayerEntity.class)
public abstract class PlayerMixin implements Powerable, Pregnant {

    @Unique
    private double power = 0;
    @Unique
    private int pregnant = 0;
    @Unique
    private short slowTick = 10;

    @Override
    public double getPower() {
        return power;
    }

    @Override
    public void setPower(double power) {
        this.power = power;
    }

    @Override
    public boolean canPowerUp() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        return player.getHungerManager().getFoodLevel() >= 3;
    }

    @Override
    public int getPregnant() {
        return pregnant;
    }

    @Override
    public void setPregnant(int pregnant) {
        this.pregnant = pregnant;
    }

    @Override
    public Entity getBaby() {
        PlayerEntity player = (PlayerEntity) (Object) this;
        var baby = new SeeeeexNekoEntity(JREntities.SEEEEEX_NEKO, player.getWorld());
        baby.setBaby(true);
        baby.age = -48000;
        baby.addOwner(player.getUuid(),new INeko.Owner(new ArrayList<>(),0));
        baby.setPos(player.getX(), player.getY(), player.getZ());
        return baby;
    }

    @Inject(method = "readCustomDataFromNbt", at = @At("HEAD"))
    public void readCustomDataFromNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        power = this.readPowerFromNbt(nbt);
        this.readPregnantFromNbt(nbt);
    }

    @Inject(method = "writeCustomDataToNbt", at = @At("HEAD"))
    public void writeCustomDataToNbt(NbtCompound nbt, CallbackInfo ci) {
        if (!((Object) this instanceof ServerPlayerEntity player)) return;
        this.writePowerToNbt(nbt);
        this.writePregnantToNbt(nbt);
    }

    @Inject(method = "tick",at = @At("HEAD"))
    public void tick(CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        Powerable.tickPower(player);
        if (slowTick++ >= 10){
            if (player instanceof ServerPlayerEntity sp) {
                // 同步power
                ServerPlayNetworking.send(sp, new PowerSyncPayload(getPower()));
            }
        }
        Pregnant.pregnantTick(player);
    }

    @Inject(method = "createPlayerAttributes", at = @At("RETURN"))
    private static void createPlayerAttributes(CallbackInfoReturnable<DefaultAttributeContainer.Builder> cir) {
        cir.getReturnValue()
                .add(Companion.getPLAYER_LUBRICATING())
                .add(Companion.getGENERIC_MAX_POWER());
    }

}
