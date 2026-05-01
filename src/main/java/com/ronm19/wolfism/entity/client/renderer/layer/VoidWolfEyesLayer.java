package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.entity.custom.elite.VoidWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import net.minecraft.world.level.Level;

public class VoidWolfEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
    private static final ResourceLocation EYES_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("wolfism", "textures/entity/void_wolf/void_wolf_eyes.png");

    public VoidWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
        super(parent);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Wolf wolf,
            float limbSwing,
            float limbSwingAmount,
            float partialTick,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (!(wolf instanceof VoidWolfEntity voidWolf)) {
            return;
        }

        if (!this.shouldGlow(voidWolf)) {
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(EYES_TEXTURE));

        this.getParentModel().renderToBuffer(
                poseStack,
                vertexConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
    }

    private boolean shouldGlow(VoidWolfEntity voidWolf) {
        if (voidWolf.isEnraged()) {
            return true;
        }

        if (voidWolf.level().dimension() == Level.END) {
            return true;
        }

        if (voidWolf.level().isNight()) {
            return true;
        }

        return voidWolf.level().getRawBrightness(voidWolf.blockPosition(), 0) <= 7;
    }
}