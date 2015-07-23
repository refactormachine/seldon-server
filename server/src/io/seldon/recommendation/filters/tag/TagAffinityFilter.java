/*
 * Seldon -- open source prediction engine
 * =======================================
 * Copyright 2011-2015 Seldon Technologies Ltd and Rummble Ltd (http://www.seldon.io/)
 *
 **********************************************************************************************
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at       
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 ********************************************************************************************** 
*/
package io.seldon.recommendation.filters.tag;

import io.seldon.clustering.recommender.RecommendationContext.OptionsHolder;
import io.seldon.general.ItemStorage;
import io.seldon.recommendation.ItemFilter;
import io.seldon.recommendation.filters.FilteredItems;
import io.seldon.recommendation.filters.tag.TagAffinityFilterModelManager.TagAffinityFilterModel;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class TagAffinityFilter implements ItemFilter {
	private static Logger logger = Logger.getLogger(TagAffinityFilter.class.getName());
	private static final String TAG_ATTR_ID_OPTION_NAME = "io.seldon.algorithm.tags.attrid";
	private final ItemStorage itemStorage;
	private final TagAffinityFilterModelManager modelManager;

	@Autowired
	public TagAffinityFilter(ItemStorage itemStorage,TagAffinityFilterModelManager modelManager){
		this.itemStorage = itemStorage;
		this.modelManager = modelManager;
	}
	 
	private boolean hasNonEmptyIntersection(Set<Integer> s1,Set<Integer> s2)
	{
		for(Integer i : s1)
		{
			if (s2.contains(i))
				return true;
		}
		return false;
	}
	
	@Override
	public List<Long> produceExcludedItems(String client, Long user,
			String clientUserId, OptionsHolder optsHolder, Long currentItem,
			String lastRecListUUID, int numRecommendations) {

		TagAffinityFilterModel model = modelManager.getClientStore(client, optsHolder);

		if (model != null)
		{
			Set<Integer> userClusters = model.userToClustersMap.get(user);
			if (userClusters != null)
			{
				if (logger.isDebugEnabled())
					logger.debug("User "+user+" tag clusters  "+userClusters.toString());
				Integer tagAttrId = optsHolder.getIntegerOption(TAG_ATTR_ID_OPTION_NAME);
				Set<Long> items = new HashSet<Long>();
				for(Map.Entry<Integer,Set<Integer>> e : model.groupToClustersMap.entrySet())
				{
					Set<Integer> clusters = e.getValue();
					if (hasNonEmptyIntersection(userClusters, clusters)) // user is in this group of clusters
					{
						for(Integer cluster : clusters)
						{
							if (!userClusters.contains(cluster)) // if user is NOT in this cluster get items to exclude for it
							{
								if (logger.isDebugEnabled())
									logger.debug("Getting recent items from cluster "+cluster+" with tags "+model.clusterToTagsMap.get(cluster).toString());
								FilteredItems fItems = itemStorage.retrieveRecentlyAddedItemsWithTags(client, 100, tagAttrId, model.clusterToTagsMap.get(cluster), cluster);
								items.addAll(fItems.getItems());
							}
						}
					}
				}
				return new ArrayList<Long>(items);
			}
		}	
			
		return new ArrayList<Long>();
		
	}

}
