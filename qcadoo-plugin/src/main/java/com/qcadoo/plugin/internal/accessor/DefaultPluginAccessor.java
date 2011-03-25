/**
 * ***************************************************************************
 * Copyright (c) 2010 Qcadoo Limited
 * Project: Qcadoo Framework
 * Version: 0.4.0
 *
 * This file is part of Qcadoo.
 *
 * Qcadoo is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation; either version 3 of the License,
 * or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA
 * ***************************************************************************
 */

package com.qcadoo.plugin.internal.accessor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.qcadoo.model.beans.qcadooPlugin.QcadooPluginPlugin;
import com.qcadoo.plugin.api.Plugin;
import com.qcadoo.plugin.api.PluginAccessor;
import com.qcadoo.plugin.api.PluginState;
import com.qcadoo.plugin.api.Version;
import com.qcadoo.plugin.internal.api.ModuleFactoryAccessor;
import com.qcadoo.plugin.internal.api.PluginDao;
import com.qcadoo.plugin.internal.api.PluginDependencyManager;
import com.qcadoo.plugin.internal.api.PluginDescriptorParser;

@Service
public final class DefaultPluginAccessor implements PluginAccessor {

    private static final Logger LOG = LoggerFactory.getLogger(DefaultPluginAccessor.class);

    @Autowired
    private PluginDescriptorParser pluginDescriptorParser;

    @Autowired
    private PluginDao pluginDao;

    @Autowired
    private PluginDependencyManager pluginDependencyManager;

    @Autowired
    private ModuleFactoryAccessor moduleFactoryAccessor;

    private final Map<String, Plugin> plugins = new HashMap<String, Plugin>();

    @Override
    public Plugin getEnabledPlugin(final String identifier) {
        Plugin plugin = plugins.get(identifier);

        if (plugin.hasState(PluginState.ENABLED)) {
            return plugin;
        } else {
            return null;
        }
    }

    @Override
    public Collection<Plugin> getEnabledPlugins() {
        Set<Plugin> enabledPlugins = new HashSet<Plugin>();

        for (Plugin plugin : plugins.values()) {
            if (plugin.hasState(PluginState.ENABLED)) {
                enabledPlugins.add(plugin);
            }
        }

        return enabledPlugins;
    }

    @Override
    public Plugin getPlugin(final String identifier) {
        return plugins.get(identifier);
    }

    @Override
    public Collection<Plugin> getPlugins() {
        return plugins.values();
    }

    @PostConstruct
    public void init() {
        LOG.info("Plugin Framework initialization");

        long time = System.currentTimeMillis();

        Set<Plugin> pluginsFromDescriptor = pluginDescriptorParser.loadPlugins();
        Set<QcadooPluginPlugin> pluginsFromDatabase = pluginDao.list();

        for (Plugin plugin : pluginsFromDescriptor) {
            QcadooPluginPlugin existingPlugin = null;
            for (QcadooPluginPlugin databasePlugin : pluginsFromDatabase) {
                if (plugin.getIdentifier().equals(databasePlugin.getIdentifier())) {
                    existingPlugin = databasePlugin;
                    break;
                }
            }
            if (existingPlugin != null) {
                plugin.changeStateTo(PluginState.valueOf(existingPlugin.getState()));
            } else {
                plugin.changeStateTo(PluginState.ENABLING);
            }

            if (existingPlugin == null || plugin.compareVersion(new Version(existingPlugin.getVersion())) > 0) {
                pluginDao.save(plugin);
            } else if (plugin.compareVersion(new Version(existingPlugin.getVersion())) < 0) {
                throw new IllegalStateException("Unsupported operation: downgrade, for plugin: " + plugin.getIdentifier());
            }

            LOG.info("Registering plugin " + plugin);

            plugins.put(plugin.getIdentifier(), plugin);
        }
        for (QcadooPluginPlugin databasePlugin : pluginsFromDatabase) {
            if (databasePlugin.getState().equals(PluginState.TEMPORARY.toString())) {
                continue;
            }

            Plugin existingPlugin = null;
            for (Plugin plugin : pluginsFromDescriptor) {
                if (databasePlugin.getIdentifier().equals(plugin.getIdentifier())) {
                    existingPlugin = plugin;
                    break;
                }
            }
            if (existingPlugin == null) {
                pluginDao.delete(databasePlugin);
            }
        }

        moduleFactoryAccessor.init();

        for (Plugin plugin : pluginDependencyManager.sortPluginsInDependencyOrder(plugins.values())) {
            plugin.init();
        }

        for (Plugin plugin : pluginDependencyManager.sortPluginsInDependencyOrder(plugins.values())) {
            if (plugin.hasState(PluginState.ENABLING)) {
                plugin.changeStateTo(PluginState.ENABLED);
                pluginDao.save(plugin);
            }
        }

        LOG.info("Plugin Framework initialized in " + (System.currentTimeMillis() - time) + "ms");
    }

    void setPluginDescriptorParser(final PluginDescriptorParser pluginDescriptorParser) {
        this.pluginDescriptorParser = pluginDescriptorParser;
    }

    void setPluginDao(final PluginDao pluginDao) {
        this.pluginDao = pluginDao;
    }

    void setPluginDependencyManager(final PluginDependencyManager pluginDependencyManager) {
        this.pluginDependencyManager = pluginDependencyManager;
    }

    void setModuleFactoryAccessor(final ModuleFactoryAccessor moduleFactoryAccessor) {
        this.moduleFactoryAccessor = moduleFactoryAccessor;
    }

    @Override
    public void savePlugin(final Plugin plugin) {
        plugins.put(plugin.getIdentifier(), plugin);
    }

    @Override
    public void removePlugin(final Plugin plugin) {
        plugins.remove(plugin.getIdentifier());
    }

}