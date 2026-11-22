package com.example.ecommerce;

public class ProductItem {
    public final long id;
    public final String name;
    public final double price;
    public final String description;
    public final String imagePath;
    public final int stockQuantity;
    public final int soldQuantity;
    public final double averageRating;
    public final int reviewCount;
    public final String categoryName;

    public ProductItem(long id, String name, double price, String description,
                       String imagePath, int stockQuantity, int soldQuantity,
                       double averageRating, int reviewCount, String categoryName) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.description = description;
        this.imagePath = imagePath;
        this.stockQuantity = stockQuantity;
        this.soldQuantity = soldQuantity;
        this.averageRating = averageRating;
        this.reviewCount = reviewCount;
        this.categoryName = categoryName;
    }
}
