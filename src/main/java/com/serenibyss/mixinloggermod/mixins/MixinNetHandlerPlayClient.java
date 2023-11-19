package com.serenibyss.mixinloggermod.mixins;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiCommandBlock;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.PacketThreadUtil;
import net.minecraft.network.ThreadQuickExitException;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.*;
import org.apache.logging.log4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Overwrite;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(NetHandlerPlayClient.class)
public class MixinNetHandlerPlayClient {

    @Shadow
    private Minecraft client;
    @Shadow @Final
    private NetworkManager netManager;
    @Shadow @Final
    private static Logger LOGGER;

    @Overwrite
    public void handleUpdateTileEntity(SPacketUpdateTileEntity packetIn) {
        try {
            NetHandlerPlayClient dis = (NetHandlerPlayClient) (Object) this;
            PacketThreadUtil.checkThreadAndEnqueue(packetIn, dis, client);

            if (client.world.isBlockLoaded(packetIn.getPos())) {
                TileEntity tileentity = client.world.getTileEntity(packetIn.getPos());
                int i = packetIn.getTileEntityType();
                boolean flag = i == 2 && tileentity instanceof TileEntityCommandBlock;

                if (i == 1 && tileentity instanceof TileEntityMobSpawner || flag ||
                        i == 3 && tileentity instanceof TileEntityBeacon ||
                        i == 4 && tileentity instanceof TileEntitySkull ||
                        i == 5 && tileentity instanceof TileEntityFlowerPot ||
                        i == 6 && tileentity instanceof TileEntityBanner ||
                        i == 7 && tileentity instanceof TileEntityStructure ||
                        i == 8 && tileentity instanceof TileEntityEndGateway ||
                        i == 9 && tileentity instanceof TileEntitySign ||
                        i == 10 && tileentity instanceof TileEntityShulkerBox ||
                        i == 11 && tileentity instanceof TileEntityBed) {
                    tileentity.readFromNBT(packetIn.getNbtCompound());
                } else {
                    if (tileentity == null) {
                        LOGGER.error("Received invalid update packet for null tile entity at {} with data: {}",
                                     packetIn.getPos(), packetIn.getNbtCompound());
                        return;
                    }
                    tileentity.onDataPacket(netManager, packetIn);
                }

                if (flag && client.currentScreen instanceof GuiCommandBlock) {
                    ((GuiCommandBlock) client.currentScreen).updateGui();
                }
            }
        } catch (Exception e) {
            if (e == ThreadQuickExitException.INSTANCE) return;
            LOGGER.error("Caught error trying to handle TE update packet at pos: " + packetIn.getPos());
            LOGGER.error("NBT: " + packetIn.getNbtCompound());
            e.printStackTrace();
        }
    }
}
