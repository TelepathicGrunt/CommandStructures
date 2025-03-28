package com.telepathicgrunt.commandstructures.services;

import com.telepathicgrunt.commandstructures.Utilities;

public interface PlatformService {
    PlatformService INSTANCE = Utilities.loadService(PlatformService.class);
}
