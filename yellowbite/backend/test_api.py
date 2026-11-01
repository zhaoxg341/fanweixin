import importlib
import os
from fastapi.testclient import TestClient

os.environ["DEMO_DATABASE_URL"] = "sqlite:///backend/test-demo.db"
from backend import main

client = TestClient(main.app)


def body(key="test-create"):
    return dict(requestId=key, shopId="rice", lines=[dict(dishId="r1", quantity=2)],
                recipient="演示同学", phone="13800138000", address="大学城学生宿舍1号楼", note="少辣")


def test_catalog_and_order_payment():
    assert client.get("/health").json()["status"] == "ok"
    assert len(client.get("/shops").json()) == 6
    response = client.post("/orders", json=body())
    assert response.status_code == 201
    order = response.json()
    assert order["subtotal"] == 4580
    assert order["total"] == 4780
    assert order["status"] in ("pending", "paid")
    assert client.post("/orders", json=body()).json()["id"] == order["id"]
    assert client.post(f"/orders/{order['id']}/pay").json()["status"] == "paid"
    assert client.post(f"/orders/{order['id']}/pay").json()["status"] == "paid"
    assert client.get(f"/orders/{order['id']}").json()["status"] == "paid"
    assert any(o["id"] == order["id"] for o in client.get("/orders").json())


def test_invalid_orders():
    value = body("invalid")
    value["lines"] = [dict(dishId="b1", quantity=1)]
    assert client.post("/orders", json=value).status_code == 400
    value["lines"] = [dict(dishId="r4", quantity=1)]
    assert client.post("/orders", json=value).status_code == 400
    value["lines"] = [dict(dishId="r1", quantity=0)]
    assert client.post("/orders", json=value).status_code == 422
    value = body("phone-invalid")
    value["phone"] = "123"
    assert client.post("/orders", json=value).status_code == 422
    value = body("counts-invalid")
    value["lines"] = [dict(dishId="r1", quantity=99), dict(dishId="r1", quantity=1)]
    assert client.post("/orders", json=value).status_code == 400
    assert client.get("/shops/unknown").status_code == 404
    assert client.post("/orders/unknown/pay").status_code == 404
