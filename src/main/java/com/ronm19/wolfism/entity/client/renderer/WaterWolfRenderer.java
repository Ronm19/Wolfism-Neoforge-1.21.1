package com.ronm19.wolfism.entity.client.renderer;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.WaterWolfCollarLayer;
import com.ronm19.wolfism.entity.client.renderer.layer.WaterWolfEyesLayer;

import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class WaterWolfRenderer extends WolfRenderer {

    private static final ResourceLocation WATER_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/water_wolf/water_wolf.png");

    public WaterWolfRenderer(EntityRendererProvider.Context context) {
        super(context);

        // Your custom layers
        this.addLayer(new WaterWolfCollarLayer(this));
        this.addLayer(new WaterWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf entity) {
        return WATER_WOLF_TEXTURE;
    }

    @Override
    public void render( Wolf entity, float entityYaw, float partialTicks, PoseStack poseStack, MultiBufferSource buffer, int packedLight ) {
        super.render(entity, entityYaw, partialTicks, poseStack, buffer, packedLight);
    }
}