package se.inera.nll.nlllight.api.medication;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "medications")
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "npl_id", unique = true, length = 20)
    private String nplId;

    @Column(name = "trade_name", nullable = false)
    private String tradeName;

    @Column(name = "generic_name")
    private String genericName;

    @Column(name = "substance_id")
    private Long substanceId;

    private String form;
    private String strength;
    private String route;

    @Column(name = "atc_code", length = 10)
    private String atcCode;

    @Column(name = "rx_status", length = 10)
    private String rxStatus;

    @Column(name = "is_available")
    private Boolean isAvailable = true;

    @Column(precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Backward compatibility
    @Transient
    public String getName() {
        return tradeName != null ? tradeName : genericName;
    }

    @Transient
    public String getDescription() {
        return form + " " + strength;
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNplId() {
        return nplId;
    }

    public void setNplId(String nplId) {
        this.nplId = nplId;
    }

    public String getTradeName() {
        return tradeName;
    }

    public void setTradeName(String tradeName) {
        this.tradeName = tradeName;
    }

    public String getGenericName() {
        return genericName;
    }

    public void setGenericName(String genericName) {
        this.genericName = genericName;
    }

    public Long getSubstanceId() {
        return substanceId;
    }

    public void setSubstanceId(Long substanceId) {
        this.substanceId = substanceId;
    }

    public String getForm() {
        return form;
    }

    public void setForm(String form) {
        this.form = form;
    }

    public String getStrength() {
        return strength;
    }

    public void setStrength(String strength) {
        this.strength = strength;
    }

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }

    public String getAtcCode() {
        return atcCode;
    }

    public void setAtcCode(String atcCode) {
        this.atcCode = atcCode;
    }

    public String getRxStatus() {
        return rxStatus;
    }

    public void setRxStatus(String rxStatus) {
        this.rxStatus = rxStatus;
    }

    public Boolean getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(Boolean isAvailable) {
        this.isAvailable = isAvailable;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    // Backward compatibility methods
    public void setName(String name) {
        this.tradeName = name;
    }

    public void setDescription(String description) {
        // No-op for backward compatibility
    }
}