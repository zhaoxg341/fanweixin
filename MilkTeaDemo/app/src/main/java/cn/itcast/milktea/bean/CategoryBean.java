package cn.itcast.milktea.bean;

/**
 * 商品分类：招牌必喝 / 鲜果茶 / 咖啡特调 / 小食甜品。
 */
public class CategoryBean {

    private final int id;
    private final String name;

    public CategoryBean(int id, String name) {
        this.id = id;
        this.name = name;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
