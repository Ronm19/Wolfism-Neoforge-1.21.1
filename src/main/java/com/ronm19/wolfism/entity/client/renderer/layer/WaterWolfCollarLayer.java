package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.entity.custom.elemental.WaterWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class WaterWolfCollarLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {

    private static final ResourceLocation WOLF_COLLAR_LOCATION =
            ResourceLocation.withDefaultNamespace("textures/entity/wolf/wolf_collar.png");

    public WaterWolfCollarLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> renderer) {
        super(renderer);
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Wolf livingEntity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (livingEntity instanceof WaterWolfEntity waterWolf && livingEntity.isTame() && !livingEntity.isInvisible()) {
            int color = waterWolf.getWaterWolfCollarColor().getTextureDiffuseColor();
            VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.entityCutoutNoCull(WOLF_COLLAR_LOCATION));

            this.getParentModel().renderToBuffer(
                    poseStack,
                    vertexConsumer,
                    packedLight,
                    OverlayTexture.NO_OVERLAY,
                    color
            );
        }
    }
}