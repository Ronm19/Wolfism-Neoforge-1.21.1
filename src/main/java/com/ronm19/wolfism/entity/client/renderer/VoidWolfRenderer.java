package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.entity.client.renderer.layer.VoidWolfEyesLayer;
import com.ronm19.wolfism.entity.custom.elite.VoidWolfEntity;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class VoidWolfRenderer extends WolfRenderer {
    private static final ResourceLocation NORMAL_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("wolfism", "textures/entity/void_wolf/void_wolf.png");

    private static final ResourceLocation ENRAGED_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("wolfism", "textures/entity/void_wolf/void_wolf_enraged.png");

    public VoidWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new VoidWolfEyesLayer(this));
        this.shadowRadius = 0.45F;
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( @NotNull Wolf wolf) {
        if (wolf instanceof VoidWolfEntity voidWolf && voidWolf.isEnraged()) {
            return ENRAGED_TEXTURE;
        }

        return NORMAL_TEXTURE;
    }

    @Override
    public void render( Wolf entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}