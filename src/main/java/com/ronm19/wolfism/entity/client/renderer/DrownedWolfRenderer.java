package com.ronm19.wolfism.entity.client.renderer;

import com.ronm19.wolfism.WolfismMod;
import com.ronm19.wolfism.entity.client.renderer.layer.DrownedWolfEyesLayer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.animal.Wolf;
import org.jetbrains.annotations.NotNull;

public class DrownedWolfRenderer extends WolfRenderer {

    private static final ResourceLocation DROWNED_WOLF_TEXTURE =
            ResourceLocation.fromNamespaceAndPath(WolfismMod.MOD_ID, "textures/entity/drowned_wolf/drowned_wolf.png");

    public DrownedWolfRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.addLayer(new DrownedWolfEyesLayer(this));
    }

    @Override
    public @NotNull ResourceLocation getTextureLocation( Wolf wolf) {
        return DROWNED_WOLF_TEXTURE;
    }
}