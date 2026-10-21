package com.campuslostfound.service;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dsa.ItemSearch;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;

import java.time.LocalDate;
import java.util.List;

/**
 * Service coordinating keyword searches, categorical filtering, and location queries.
 */
public class SearchService {

    private final ItemDAO itemDAO;

    public SearchService() {
        this(new ItemDAO());
    }

    public SearchService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    /**
     * Executes a multi-parameter database search.
     */
    public List<Item> search(String keyword, String category, Integer locationId, ItemType type, ItemStatus status) {
        return itemDAO.search(keyword, category, locationId, type, status);
    }

    /**
     * Quick search by text keyword across all active items.
     */
    public List<Item> quickSearch(String keyword) {
        return itemDAO.search(keyword, null, null, null, ItemStatus.ACTIVE);
    }

    /**
     * Loads items into an in-memory ItemSearch collection for high-performance DSA operations.
     */
    public ItemSearch createInMemorySearchIndex() {
        ItemSearch index = new ItemSearch();
        List<Item> allItems = itemDAO.findAll();
        index.addAll(allItems);
        return index;
    }

    /**
     * Searches items in-memory using the provided ItemSearch index.
     */
    public List<Item> searchInMemory(ItemSearch index, String keyword, String category, LocalDate start, LocalDate end) {
        if (index == null) {
            return List.of();
        }
        return index.search(keyword, category, start, end);
    }
}
