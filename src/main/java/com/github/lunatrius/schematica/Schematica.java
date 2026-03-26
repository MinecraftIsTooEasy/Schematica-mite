// MITE port (c) 2025 hahahha. Licensed under the MIT License.
package com.github.lunatrius.schematica;

import com.github.lunatrius.schematica.reference.Reference;
import com.github.lunatrius.worldgen.SchematicWorldgenRegistration;
import moddedmite.rustedironcore.api.event.Handlers;
import net.fabricmc.api.ModInitializer;
import net.xiaoyu233.fml.ModResourceManager;
import net.xiaoyu233.fml.reload.event.MITEEvents;

public class Schematica implements ModInitializer {
    @Override
    public void onInitialize() {
        ModResourceManager.addResourcePackDomain("schematica");
        MITEEvents.MITE_EVENT_BUS.register(new SchematicaEventListener());

        Handlers.BiomeDecoration.registerPre(SchematicWorldgenRegistration::registerExample);

        Reference.logger.info("Schematica core initialized");
    }
}
