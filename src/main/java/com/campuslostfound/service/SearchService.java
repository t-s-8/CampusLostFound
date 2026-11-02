package com.campuslostfound.service;

import com.campuslostfound.dao.ItemDAO;
import com.campuslostfound.dsa.ItemSearch;
import com.campuslostfound.model.Item;
import com.campuslostfound.model.ItemStatus;
import com.campuslostfound.model.ItemType;

import java.time.LocalDate;
import java.util.List;

public class SearchService {

    private final ItemDAO itemDAO;

    public SearchService() {
        this(new ItemDAO());
    }

    public SearchService(ItemDAO itemDAO) {
        this.itemDAO = itemDAO;
    }

    public List<Item> search(String keyword, String category, Integer locationId, ItemType type, ItemStatus status) {
        return itemDAO.search(keyword, category, locationId, type, status);
    }

    public List<Item> quickSearch(String keyword) {
        return itemDAO.search(keyword, null, null, null, ItemStatus.ACTIVE);
    }

    public ItemSearch createInMemorySearchIndex() {
        ItemSearch index = new ItemSearch();
        List<Item> allItems = itemDAO.findAll();
        index.addAll(allItems);
        return index;
    }

    public List<Item> searchInMemory(ItemSearch index, String keyword, String category, LocalDate start, LocalDate end) {
        if (index == null) {
            return List.of();
        }
        return index.search(keyword, category, start, end);
    }
}
