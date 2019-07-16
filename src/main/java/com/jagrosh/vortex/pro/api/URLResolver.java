package com.jagrosh.vortex.pro.api;

import com.jagrosh.vortex.pro.ProFeature;
import com.jagrosh.vortex.pro.api.mocks.URLResolverMock;

import java.util.List;

@ProFeature(value = "com.jagrosh.vortex.pro.URLResolver", mock = URLResolverMock.class)
public interface URLResolver
{
    void loadSafeDomains();

    List<String> findRedirects(String link);
}
