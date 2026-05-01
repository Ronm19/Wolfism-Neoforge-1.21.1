package com.ronm19.wolfism.entity.client.renderer;


import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class TimberWolfRenderer extends WolfRenderer {

    private static final ResourceLocation NORMAL =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/timber_wolf/timber_wolf.png");

    private static final ResourceLocation ANGRY =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/timber_wolf/timber_wolf_angry.png");

    public TimberWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf wolf) {
        return wolf.isAngry() ? ANGRY : NORMAL;
    }

    @Override
    public void render( Wolf entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}