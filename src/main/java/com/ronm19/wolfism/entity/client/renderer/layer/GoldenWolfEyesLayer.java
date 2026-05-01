package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.special.GoldenWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;

public class GoldenWolfEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
    private static final ResourceLocation GOLDEN_WOLF_EYES =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/golden_wolf/golden_wolf_eyes.png");

    public GoldenWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> parent) {
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
        if (!(wolf instanceof GoldenWolfEntity goldenWolf)) {
            return;
        }

        if (!goldenWolf.shouldGlowEyes()) {
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(GOLDEN_WOLF_EYES));

        this.getParentModel().renderToBuffer(
                poseStack,
                vertexConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
    }
}