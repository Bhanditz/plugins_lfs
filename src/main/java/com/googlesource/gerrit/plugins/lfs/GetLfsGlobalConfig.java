// Copyright (C) 2016 The Android Open Source Project
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.googlesource.gerrit.plugins.lfs;

import static com.google.gerrit.server.permissions.GlobalPermission.ADMINISTRATE_SERVER;

import com.google.common.collect.Maps;
import com.google.gerrit.extensions.restapi.ResourceNotFoundException;
import com.google.gerrit.extensions.restapi.RestApiException;
import com.google.gerrit.extensions.restapi.RestReadView;
import com.google.gerrit.server.CurrentUser;
import com.google.gerrit.server.IdentifiedUser;
import com.google.gerrit.server.config.AllProjectsName;
import com.google.gerrit.server.permissions.PermissionBackend;
import com.google.gerrit.server.project.ProjectResource;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import java.util.HashMap;
import java.util.List;

@Singleton
class GetLfsGlobalConfig implements RestReadView<ProjectResource> {
  private final LfsConfigurationFactory lfsConfigFactory;
  private final AllProjectsName allProjectsName;
  private final Provider<CurrentUser> self;
  private final PermissionBackend permissionBackend;

  @Inject
  GetLfsGlobalConfig(
      LfsConfigurationFactory lfsConfigFactory,
      AllProjectsName allProjectsName,
      Provider<CurrentUser> self,
      PermissionBackend permissionBackend) {
    this.lfsConfigFactory = lfsConfigFactory;
    this.allProjectsName = allProjectsName;
    this.self = self;
    this.permissionBackend = permissionBackend;
  }

  @Override
  public LfsGlobalConfigInfo apply(ProjectResource resource) throws RestApiException {
    IdentifiedUser user = self.get().asIdentifiedUser();
    if (!(resource.getNameKey().equals(allProjectsName)
        && permissionBackend.user(user).testOrFalse(ADMINISTRATE_SERVER))) {
      throw new ResourceNotFoundException();
    }

    LfsGlobalConfigInfo info = new LfsGlobalConfigInfo();
    LfsGlobalConfig globalConfig = lfsConfigFactory.getGlobalConfig();
    info.defaultBackendType = globalConfig.getDefaultBackend().type;
    info.backends = Maps.transformValues(globalConfig.getBackends(), b -> b.type);

    List<LfsProjectConfigSection> configSections =
        lfsConfigFactory.getProjectsConfig().getConfigSections();
    if (!configSections.isEmpty()) {
      info.namespaces = new HashMap<>(configSections.size());
      for (LfsProjectConfigSection section : configSections) {
        LfsProjectConfigInfo sectionInfo = new LfsProjectConfigInfo();
        sectionInfo.enabled = section.isEnabled();
        sectionInfo.maxObjectSize = section.getMaxObjectSize();
        sectionInfo.readOnly = section.isReadOnly();
        sectionInfo.backend = section.getBackend();
        info.namespaces.put(section.getNamespace(), sectionInfo);
      }
    }
    return info;
  }
}
