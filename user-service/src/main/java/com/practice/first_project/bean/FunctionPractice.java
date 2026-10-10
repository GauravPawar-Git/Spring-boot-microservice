package com.practice.first_project.bean;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Plain Java practice (streams, bubble sort, selection sort), unrelated to the Spring app.
 * Nothing calls it; run its main() directly from the IDE.
 */
public class FunctionPractice {
    public static void main(String args[]) {

        // create a list of strings
        List<String> n = Arrays.asList("Geek", "GeeksQuiz", "g1", "QA", "Geek2", "QA", "Geek2");
        List<String> names = Arrays.asList("Geek Kent", "John Doe", "Larry White", "Eva Genger", "Molly Robbie");

//        Stream.concat(n.stream(), names.stream()).forEach(str -> System.out.print(" "+str));
//        Predicate<String> predicate = s -> s.startsWith("Q");

//        printVal(names.stream().map(s -> s.split(" ")[1]).collect(Collectors.toList()));
//        printVal(names.stream().map(s -> s.split(" ")[1]).collect(Collectors.toList()));
//        n.stream().filter(predicate).forEach(System.out::println);

//        printVal(n.stream().map(String::toUpperCase).collect(Collectors.toList()));
//        Set set = new HashSet();
//        n.stream().filter(str -> !set.add(str)).forEach(str -> System.out.print(str));

        bubbleSortArr();
//        selectionSort();
//        printVal(getEvenNumbers());

    }

    public static List<Integer> getEvenNumbers() {
        List<Integer> integerList = Arrays.asList(10, 22, 3, 24, 65, 36, 97, 8, 9, 11, 12, 13, 14);

        IntSummaryStatistics intSummaryStatistics = integerList.stream().mapToInt(Integer::intValue).summaryStatistics();

        Optional number = integerList.stream().max(Integer::compare);
        int num2 = integerList.stream().mapToInt(Integer::intValue).sum();
        boolean bool = integerList.stream().allMatch(n -> n > 50);
//        System.out.println(num2 + "  " + number + " " + bool);
        System.out.println("peek = " + intSummaryStatistics.toString());
//        return integerList.stream().filter(n -> n%2 == 0).collect(Collectors.toList());
        return integerList.stream().sorted().collect(Collectors.toList());

    }

    public static void printVal(List anyList) {
        anyList.stream().forEach(System.out::println);
    }

    public static void printVal(Set anyList) {
        anyList.stream().forEach(System.out::println);
    }

    public static void bubbleSortArr() {

        int arr[] = {7, 8, 1, 4, 3};


        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - i - 1; j++) {
//                System.out.println(arr[j] + "");
                if (arr[j] > arr[j + 1]) {

                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
                printArr(arr);
            }

        }

        System.out.println("Second largest no = "+arr[arr.length - 2]);
    }

    public static void printArr(int[] arr) {
        for (int j = 0; j < arr.length; j++) {
            System.out.print(" = " + arr[j] + "");
        }
        System.out.println("  ");
    }

    public static void selectionSort() {

        int arr[] = {5, 3, 8, 9, 4, 2};

        for (int i = 0; i < arr.length - 1; i++) {
            int smallest = i;
            for (int j = i+1; j < arr.length; j++) {
                if (arr[smallest] > arr[j]) {
                    smallest = j;
                }
            }
            int temp = arr[smallest];
            arr[smallest] = arr[i];
            arr[i] = temp;

            printArr(arr);

        }

//        printArr(arr);
    }
}
