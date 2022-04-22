package com.telepathicgrunt.commandstructures;

import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.config.Node;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.filter.AbstractFilter;
import org.apache.logging.log4j.message.Message;

@Plugin(name = "LogSpamFiltering", category = Node.CATEGORY, elementType = Filter.ELEMENT_TYPE)
public class LogSpamFiltering extends AbstractFilter {

    @Override
    public Result filter(LogEvent event) {
        Message message = event.getMessage();
        if (message != null) {
            if(message.getFormattedMessage().contains("Trying to mark a block for PostProcessing @")) {
                return Result.DENY;
            }
        }

        return Result.NEUTRAL;
    }
}