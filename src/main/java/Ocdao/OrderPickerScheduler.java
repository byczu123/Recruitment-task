package Ocdao;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.util.*;

public class OrderPickerScheduler {
    public static void main(String[] args) {
        try {
            int i = 0;
            String iString = Integer.toString(i);
            String jsonStoreFile = args[0];
            String jsonOrderFile = args[1];

            // create ObjectMapper instance
            ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.registerModule(new JavaTimeModule());

            // Wczytywanie i parsowanie
            File storeFile = new File(jsonStoreFile);
            Store store = objectMapper.readValue(storeFile, Store.class);

            // read and parse order JSON file
            File orderFile = new File(jsonOrderFile);
            ArrayList<Order> orderList = new ArrayList<>(
                    Arrays.asList(objectMapper.readValue(orderFile, Order[].class)));



            //Sortowanie po ratio Wartość orderu/Czas pickowania orderu
            orderList.sort(new Comparator<Order>() {
                public int compare(Order o1, Order o2) {

                    return Float.compare(o2.getOrderValue(), o1.getOrderValue());

                }
            });
            /** Zadanie 2 * Sortowanie po ratio Wartość orderu/Czas pickowania orderu, żeby ruszyć należy zakomentować linie 31-36 i odkomentować linie 41-57 */
            /*orderList.sort(new Comparator<Order>() {
                public int compare(Order o1, Order o2) {

                    int ratioMinutes1 =(int) o1.getPickingTime().toMinutes();
                    int ratioMinutes2 =(int) o2.getPickingTime().toMinutes();
                    float ratio1 = o1.getOrderValue()/ ratioMinutes1;
                    float ratio2 = o2.getOrderValue()/ ratioMinutes2;

                    if (ratio1 == ratio2) {
                        return Float.compare(o2.getOrderValue(), o1.getOrderValue()); // sort by orderValue if ratios are equal
                    }
                    return Float.compare(ratio2,ratio1);
                }
            });
            for(Order order : orderList){
                System.out.println("order id: "+ order.getOrderId() + "picking time: "+ order.getPickingTime() + "order value: "+ order.getOrderValue());
            } */



            /*for(Order order : orderList){
                System.out.println("order id: "+ order.getOrderId() + "picking time: "+ order.getPickingTime() + "order value: "+ order.getOrderValue());
            }*/

            int time = store.availibilityTable();
            int workers = store.getPickers().size();
            Boolean[][] timeSlots = store.createTimeTable(time,workers);
            store.assignOrdersToWorkers(orderList, timeSlots,store.getPickingStartTime());

        }
        catch (IOException e){
            e.printStackTrace();
        }

    }

}


class Order{
    private String orderId;
    private float orderValue;
    private Duration pickingTime;
    private String completeBy;

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public float getOrderValue() {
        return orderValue;
    }

    public void setOrderValue(float orderValue) {
        this.orderValue = orderValue;
    }

    public Duration getPickingTime() {
        return pickingTime;
    }

    public void setPickingTime(Duration pickingTime) {
        this.pickingTime = pickingTime;
    }

    public String getCompleteBy() {
        return completeBy;
    }

    public void setCompleteBy(String completeBy) {
        this.completeBy = completeBy;
    }
}


class Store {
    private List<String> pickers;
    private LocalTime pickingStartTime;
    private LocalTime pickingEndTime;

    public List<String> getPickers() {
        return pickers;
    }

    public void setPickers(List<String> pickers) {
        this.pickers = pickers;
    }

    public LocalTime getPickingStartTime() {
        return pickingStartTime;
    }

    public void setPickingStartTime(LocalTime pickingStartTime) {
        this.pickingStartTime = pickingStartTime;
    }

    public LocalTime getPickingEndTime() {
        return pickingEndTime;
    }

    public void setPickingEndTime(LocalTime pickingEndTime) {
        this.pickingEndTime = pickingEndTime;
    }

    public int availibilityTable() {
        LocalTime pickingStartTime = this.pickingStartTime;
        LocalTime pickingEndTime = this.pickingEndTime;
        Duration duration = Duration.between(pickingStartTime, pickingEndTime);
        long seconds = duration.getSeconds();
        return (int) seconds / 60;
    }


    //Tworzenie listy dla kazdego z pracownikow
    public Boolean[][] createTimeTable(int minutes, int amountOfWorkers) {
        Boolean[][] timeTable = new Boolean[amountOfWorkers][minutes];
        for (int workers = 0; workers < amountOfWorkers; workers++) {
            for (int time = 0; time < minutes; time++) {
                timeTable[workers][time] = true;
            }
        }
        return timeTable;
    }


    //Algorytm przypisujący zamówienia do pracowników
    public void assignOrdersToWorkers(ArrayList<Order> orders, Boolean[][] timeSlots, LocalTime pickingStartTime) {
        //Zmienna do zbierania ilosci zamowien
        int collectedOrders=0;
        //Zmienna do zebrania sumarycznej wartości zamowien
        float collectedValue=0;
        for (Order order : orders) {
            //
            boolean orderPacked = false;
            //Iteracja po pracownikach
            for (int workerI = 0; workerI < timeSlots.length; workerI++) {
                if(orderPacked){
                    break;
                }
                //Iteracja po każdym slocie czasowym pracownika
                for (int timeSlotI = 0; timeSlotI <= timeSlots[workerI].length; timeSlotI++) {
                    if(orderPacked){
                        break;
                    }
                    boolean ableToPack = true;
                    //czas trwania zbierania zamowienia
                    int duration = (int) order.getPickingTime().getSeconds() / 60;
                    //przewidywany czas potencjalnego zakonczenia pakowania
                    int predictedEnd = timeSlotI + duration;
                    //Sprawdzenie czy na danym slocie czasowym mozna rozpoczac pakowanie
                    if(predictedEnd < timeSlots[workerI].length) {
                        for (int tempTimeSlotI = timeSlotI; tempTimeSlotI < predictedEnd; tempTimeSlotI++) {

                            if(timeSlots[workerI][tempTimeSlotI]==false){
                                //Jesli pracownik w ktoryms ze sprawdzanych slotow jest zajety, przerwij sprawdzanie.
                                ableToPack=false;
                                break;
                            }

                        }
                        //Pracownik dostepny do pakowania, przypisujemy go do zadania(rym :) )
                        if(ableToPack==true){
                            for (int tempTimeSlotI = timeSlotI; tempTimeSlotI < predictedEnd; tempTimeSlotI++) {
                                timeSlots[workerI][tempTimeSlotI]=false;


                            }
                            orderPacked = true;
                            //Ilosc zebranych zamowien
                            //collectedOrders++;
                            collectedValue+=order.getOrderValue();
                            //System.out.println(collectedOrders);
                            //Początkowy czas zbierania zamowienia
                            LocalTime pickingStart = pickingStartTime.plusMinutes(timeSlotI);
                            System.out.println("Picker: "+workerI + " " + order.getOrderId() + " " + pickingStart);
                        }

                    }else {
                        break;
                    }

                }
            }
        }
        System.out.println(collectedValue);
    }
}
