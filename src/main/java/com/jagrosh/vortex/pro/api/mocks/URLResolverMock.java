package com.jagrosh.vortex.pro.api.mocks;

import com.jagrosh.vortex.pro.api.URLResolver;

import java.util.Collections;
import java.util.List;

public class URLResolverMock implements URLResolver
{
    public URLResolverMock(String url, String secret){}

    @Override
    public void loadSafeDomains(){}

    @Override
    public List<String> findRedirects(String link)
    {
        return Collections.emptyList();
    }
}
