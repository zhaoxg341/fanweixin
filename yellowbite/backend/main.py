"""Small, persistent API for the Android demo. Prices are integer cents."""
from __future__ import annotations

import json
import os
import uuid
from datetime import datetime, timezone
from pathlib import Path
from typing import Annotated

from fastapi import FastAPI, HTTPException
from fastapi.staticfiles import StaticFiles
from pydantic import BaseModel, Field
from sqlalchemy import String, Text, create_engine, select
from sqlalchemy.orm import DeclarativeBase, Mapped, Session, mapped_column
from sqlalchemy.exc import IntegrityError

ROOT = Path(__file__).parent
CATALOG = json.loads((ROOT / "catalog.json").read_text(encoding="utf-8"))
engine = create_engine(os.getenv("DEMO_DATABASE_URL", f"sqlite:///{ROOT / 'demo.db'}"), connect_args={"check_same_thread": False})


class Base(DeclarativeBase):
    pass


class OrderRow(Base):
    __tablename__ = "orders"
    id: Mapped[str] = mapped_column(String, primary_key=True)
    request_id: Mapped[str] = mapped_column(String, unique=True)
    payload: Mapped[str] = mapped_column(Text)


Base.metadata.create_all(engine)
app = FastAPI(title="小黄外卖 Demo API", version="1.0.0")
images = ROOT.parent / "app/src/main/assets/food"
if images.exists():
    app.mount("/images", StaticFiles(directory=images), name="images")


class OrderLine(BaseModel):
    dishId: str
    quantity: Annotated[int, Field(ge=1, le=99)]


class CreateOrder(BaseModel):
    requestId: Annotated[str, Field(min_length=1, max_length=100)]
    shopId: str
    lines: Annotated[list[OrderLine], Field(min_length=1, max_length=100)]
    recipient: Annotated[str, Field(min_length=1, max_length=40)]
    phone: Annotated[str, Field(pattern=r"^1[3-9]\d{9}$")]
    address: Annotated[str, Field(min_length=5, max_length=200)]
    note: Annotated[str, Field(max_length=200)] = ""


@app.get("/health")
def health():
    return {"status": "ok", "payment": "simulation"}


@app.get("/shops")
def shops():
    return CATALOG


@app.get("/shops/{shop_id}")
def shop(shop_id: str):
    value = next((s for s in CATALOG if s["id"] == shop_id), None)
    if value is None:
        raise HTTPException(404, "店铺不存在")
    return value


@app.post("/orders", status_code=201)
def create_order(body: CreateOrder):
    with Session(engine) as session:
        existing = session.scalar(select(OrderRow).where(OrderRow.request_id == body.requestId))
        if existing:
            return json.loads(existing.payload)
        store = shop(body.shopId)
        dishes = {d["id"]: d for d in store["dishes"]}
        counts: dict[str, int] = {}
        for line in body.lines:
            if line.dishId not in dishes:
                raise HTTPException(400, "菜品不属于当前店铺")
            counts[line.dishId] = counts.get(line.dishId, 0) + line.quantity
            if counts[line.dishId] > 99:
                raise HTTPException(400, "单个菜品最多购买99份")
        items = [dict(dishId=id, name=dishes[id]["name"], price=dishes[id]["price"], quantity=q, image=dishes[id]["image"]) for id, q in counts.items()]
        subtotal = sum(i["price"] * i["quantity"] for i in items)
        if subtotal < store["minimum"]:
            raise HTTPException(400, "未达到起送金额")
        order = dict(id="YB" + uuid.uuid4().hex[:12].upper(), shopId=store["id"], shopName=store["name"],
                     items=items, subtotal=subtotal, deliveryFee=store["deliveryFee"], total=subtotal + store["deliveryFee"],
                     status="pending", createdAt=datetime.now(timezone.utc).isoformat(),
                     recipient=body.recipient.strip(), phone=body.phone, address=body.address.strip(), note=body.note.strip(), remoteUrl=None)
        session.add(OrderRow(id=order["id"], request_id=body.requestId, payload=json.dumps(order, ensure_ascii=False)))
        try:
            session.commit()
        except IntegrityError:
            session.rollback()
            return json.loads(session.scalar(select(OrderRow).where(OrderRow.request_id == body.requestId)).payload)
        return order


@app.get("/orders")
def orders():
    with Session(engine) as session:
        values = [json.loads(r.payload) for r in session.scalars(select(OrderRow))]
        return sorted(values, key=lambda r: r["createdAt"], reverse=True)


@app.get("/orders/{order_id}")
def get_order(order_id: str):
    with Session(engine) as session:
        row = session.get(OrderRow, order_id)
        if not row:
            raise HTTPException(404, "订单不存在")
        return json.loads(row.payload)


@app.post("/orders/{order_id}/pay")
def pay(order_id: str):
    """Demo only: records payment state, never charges money."""
    with Session(engine) as session:
        row = session.get(OrderRow, order_id)
        if not row:
            raise HTTPException(404, "订单不存在")
        order = json.loads(row.payload)
        order["status"] = "paid"
        row.payload = json.dumps(order, ensure_ascii=False)
        session.commit()
        return order
