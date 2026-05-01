package com.ronm19.wolfism.entity.client.renderer.layer;

import com.ronm19.wolfism.WolfismMod;
import net.minecraft.client.model.WolfModel;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.entity.custom.elemental.FrostWolfEntity;
import org.jetbrains.annotations.NotNull;

public class FrostWolfEyesLayer extends EyesLayer<Wolf, WolfModel<Wolf>> {

    private static final RenderType FROST_WOLF_EYES = RenderType.eyes(
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/frost_wolf/frost_wolf_eyes.png")
    );

    public FrostWolfEyesLayer(RenderLayerParent<Wolf, WolfModel<Wolf>> renderer) {
        super(renderer);
    }

    @Override
    public @NotNull RenderType renderType() {
        return FROST_WOLF_EYES;
    }

    @Override
    public void render(
            PoseStack poseStack,
            MultiBufferSource buffer,
            int packedLight,
            Wolf entity,
            float limbSwing,
            float limbSwingAmount,
            float partialTicks,
            float ageInTicks,
            float netHeadYaw,
            float headPitch
    ) {
        if (entity instanceof FrostWolfEntity frostWolf && frostWolf.isGlowingEyes()) {

            super.render(
                    poseStack,
                    buffer,
                    15728640, // 🔥 FULL BRIGHT (this is the fix)
                    entity,
                    limbSwing,
                    limbSwingAmount,
                    partialTicks,
                    ageInTicks,
                    netHeadYaw,
                    headPitch
            );
        }
    }
}