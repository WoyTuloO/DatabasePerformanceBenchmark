package com.example.performanceTesting.adapters.out.persistence.postgres.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
@Entity
@Table(name = "order_items", schema = "shop")
public class OrderItemEntity {

    @EmbeddedId
    private OrderItemId id;

    @Column(name = "product_id", nullable = false)
    private Long productId;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "unit_price_cents", nullable = false)
    private Integer unitPriceCents;

    @Embeddable
    public static class OrderItemId implements Serializable {

        @Column(name = "order_id")
        private Long orderId;

        @Column(name = "line_no")
        private Integer lineNo;

        public OrderItemId() {
        }

        public OrderItemId(Long orderId, Integer lineNo) {
            this.orderId = orderId;
            this.lineNo = lineNo;
        }

        public Long getOrderId() {
            return orderId;
        }

        public Integer getLineNo() {
            return lineNo;
        }

        public void setOrderId(Long orderId) {
            this.orderId = orderId;
        }

        public void setLineNo(Integer lineNo) {
            this.lineNo = lineNo;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null || getClass() != obj.getClass()) {
                return false;
            }
            OrderItemId that = (OrderItemId) obj;
            if (orderId != null ? !orderId.equals(that.orderId) : that.orderId != null) {
                return false;
            }
            return lineNo != null ? lineNo.equals(that.lineNo) : that.lineNo == null;
        }

        @Override
        public int hashCode() {
            int result = orderId != null ? orderId.hashCode() : 0;
            result = 31 * result + (lineNo != null ? lineNo.hashCode() : 0);
            return result;
        }
    }
}
