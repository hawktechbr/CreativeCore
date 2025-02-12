package team.creative.creativecore.common.util.mc;

import net.minecraft.world.level.Level;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import team.creative.creativecore.client.CreativeCoreClient;

public class TickUtils {
    
    @OnlyIn(Dist.CLIENT)
    private static float getDeltaFrameTimeClient() {
        return CreativeCoreClient.getDeltaFrameTime();
    }
    
    public static float getDeltaFrameTime(Level level) {
        if (level.isClientSide)
            return getDeltaFrameTimeClient();
        return 1.0F;
    }
    
}
