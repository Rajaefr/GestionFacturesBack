package ma.Controle.gestionFactures.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentCategorySummary {

    private String category;
    private double totalAmount; // This could be fetched from the 'facture' entity or predefined
    private double paidAmount;
    private double remainingAmount;

    public PaymentCategorySummary(String category) {
        this.category = category;
        this.paidAmount = 0;
        this.remainingAmount = 0;
        this.totalAmount = 0;  // Initialize totalAmount if it's not set in the constructor
    }

    public void addPaidAmount(double paidAmount) {
        this.paidAmount += paidAmount;
    }

    // Getter and Setter methods will be automatically generated by Lombok

}
