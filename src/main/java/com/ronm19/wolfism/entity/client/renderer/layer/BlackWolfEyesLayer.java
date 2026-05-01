package com.ronm19.wolfism.entity.client.renderer.layer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.custom.neutral.BlackWolfEntity;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class BlackWolfEyesLayer extends RenderLayer<Wolf, WolfModel<Wolf>> {

    private static final ResourceLocation BLACK_WOLF_EYES =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/black_wolf/black_wolf_eyes.png");

    public BlackWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> renderer) {
        super(renderer);
    }

    @Override
    public void render( @NotNull PoseStack poseStack,
                        @NotNull MultiBufferSource buffer,
                        int packedLight,
                        @NotNull Wolf entity,
                        float limbSwing,
                        float limbSwingAmount,
                        float partialTick,
                        float ageInTicks,
                        float netHeadYaw,
                        float headPitch) {

        // 🔥 Cast to your custom entity
        if (!(entity instanceof BlackWolfEntity blackWolf)) {
            return;
        }

        if (!blackWolf.isNightActive() || blackWolf.isInvisible()) {
            return;
        }

        VertexConsumer vertexConsumer = buffer.getBuffer(RenderType.eyes(BLACK_WOLF_EYES));

        this.getParentModel().renderToBuffer(
                poseStack,
                vertexConsumer,
                15728640, // full bright
                OverlayTexture.NO_OVERLAY
        );
    }
}