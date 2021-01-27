package main

import (
	"errors"
	"fmt"
	"io/ioutil"
	"log"
	"os"
	"sort"
	"strconv"
	"strings"

	neuraln "github.com/9init/NeuralNetworkGo"

	"github.com/9init/NeuralNetworkGo/iprocessing"
)

func main() {
	if os.Args[1] == "predict" {
		predict()
	} else if os.Args[1] == "train" {
		train()
	} else {
		handelError(errors.New("wrong parameters"))
	}
}

func handelError(err error) {
	if err != nil {
		log.Fatal(err)
	}
}

func predict() {
	data, err := ioutil.ReadFile("/data/data/com.lilHesham.ecocycle/binaries/trained-Data.json")
	handelError(err)
	nn, err := neuraln.ImportJSON(data)
	handelError(err)

	plasticList := []string{
		"polyethylene_PET",
		"high_density_polyethylene_PEHD",
		"polyvinylchloride_PVC",
		"low_density_polyethylene_PELD",
		"polypropylene_PP",
		"polystyrene_PS",
		"other_resins",
		"not a plastic",
	}

	// ./file predict image
	ln := os.Args[2]
	imgdata := iprocessing.SetUpTrainingData(ln, 200, 200)
	output := nn.FeedForword(*imgdata)
	outData := make([]float64, 0)

	for _, v := range output.Matrix {
		outData = append(outData, v[0])
	}

	plastics := make([]string, 0)
	sort.Float64s(outData)

	for i := 7; i >= 5; i-- {
		for j, outputV := range output.Matrix {
			if outData[i] == outputV[0] {
				plastics = append(plastics, plasticList[j])
			}
		}
	}

	for _, v := range output.Matrix {
		if v[0] == outData[len(outData)-1] {
			fmt.Printf("%0.18f,%s,%0.18f,%s,%0.18f,%s",
				outData[7], plastics[0], outData[6], plastics[1], outData[5], plastics[2])
			break
		}
	}
}

func train() {

	data, err := ioutil.ReadFile("/data/data/com.lilHesham.ecocycle/binaries/trained-Data.json")
	handelError(err)
	nn, err := neuraln.ImportJSON(data)
	handelError(err)
	//	0	  1	    2	  3
	// ./file train image output
	ln := os.Args[2]
	outputs := strings.Split(os.Args[3], ",")
	imgdata := iprocessing.SetUpTrainingData(ln, 200, 200)
	for i := 0; i < 10; i++ {
		nn.Train(*imgdata, stringToFloat64(outputs))
	}

	jsonData, err := nn.ExportJSON()
	handelError(err)
	fs, err := os.Open("/data/data/com.lilHesham.ecocycle/binaries/trained-Data.json")
	fs.Truncate(0)
	handelError(err)
	fs.Write(jsonData)
}

func stringToFloat64(strs []string) []float64 {
	float64s := make([]float64, 8)
	for i, v := range strs {
		num, err := strconv.Atoi(v)
		handelError(err)
		float64s[i] = float64(num)
	}
	return float64s
}
