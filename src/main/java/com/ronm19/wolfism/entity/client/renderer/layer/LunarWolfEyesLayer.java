package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.elite.LunarWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class LunarWolfEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {
    private static final ResourceLocation LUNAR_WOLF_EYES =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/lunar_wolf/lunar_wolf_eyes.png");

    public LunarWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> renderer) {
        super(renderer);
    }

    @Override
    public void render( @NotNull PoseStack poseStack,
                        @NotNull MultiBufferSource bufferSource,
                        int packedLight,
                        @NotNull Wolf wolf,
                        float limbSwing,
                        float limbSwingAmount,
                        float partialTick,
                        float ageInTicks,
                        float netHeadYaw,
                        float headPitch) {
        if (!(wolf instanceof LunarWolfEntity lunarWolf)) {
            return;
        }

        if (!lunarWolf.shouldGlowEyes()) {
            return;
        }

        VertexConsumer vertexConsumer = bufferSource.getBuffer(RenderType.eyes(LUNAR_WOLF_EYES));

        this.getParentModel().renderToBuffer(
                poseStack,
                vertexConsumer,
                LightTexture.FULL_BRIGHT,
                OverlayTexture.NO_OVERLAY,
                0xFFFFFFFF
        );
    }
}