package main

import (
	"fmt"
	"io/ioutil"
	"log"
	"math/rand"
	"os"
	"time"

	neuraln "github.com/9init/NeuralNetworkGo"

	"github.com/9init/NeuralNetworkGo/iprocessing"
)

type objects struct {
	path    string
	file    []os.FileInfo
	outputs []float64
}

type files struct {
	fName   string
	outputs []float64
}

func shuffle(l *[]files) {
	rand.Seed(time.Now().UnixNano())
	time.Sleep(1)
	rand.Shuffle(len(*l), func(i, j int) { (*l)[i], (*l)[j] = (*l)[j], (*l)[i] })
}

func main() {
	polyethylene_PET_files, err := ioutil.ReadDir("./seven_plastics/1_polyethylene_PET")
	handelError(err)
	polyethylene_PET := objects{"./seven_plastics/1_polyethylene_PET", polyethylene_PET_files, []float64{1, 0, 0, 0, 0, 0, 0, 0}}

	high_density_polyethylene_PEHD_files, err := ioutil.ReadDir("./seven_plastics/2_high_density_polyethylene_PE-HD")
	handelError(err)
	high_density_polyethylene_PEHD := objects{"./seven_plastics/2_high_density_polyethylene_PE-HD", high_density_polyethylene_PEHD_files, []float64{0, 1, 0, 0, 0, 0, 0, 0}}

	polyvinylchloride_PVC_files, err := ioutil.ReadDir("./seven_plastics/3_polyvinylchloride_PVC")
	handelError(err)
	polyvinylchloride_PVC := objects{"./seven_plastics/3_polyvinylchloride_PVC", polyvinylchloride_PVC_files, []float64{0, 0, 1, 0, 0, 0, 0, 0}}

	low_density_polyethylene_PELD_files, err := ioutil.ReadDir("./seven_plastics/4_low_density_polyethylene_PE-LD")
	handelError(err)
	low_density_polyethylene_PELD := objects{"./seven_plastics/4_low_density_polyethylene_PE-LD", low_density_polyethylene_PELD_files, []float64{0, 0, 0, 1, 0, 0, 0, 0}}

	polypropylene_PP_files, err := ioutil.ReadDir("./seven_plastics/5_polypropylene_PP")
	handelError(err)
	polypropylene_PP := objects{"./seven_plastics/5_polypropylene_PP", polypropylene_PP_files, []float64{0, 0, 0, 0, 1, 0, 0, 0}}

	polystyrene_PS_files, err := ioutil.ReadDir("./seven_plastics/6_polystyrene_PS")
	handelError(err)
	polystyrene_PS := objects{"./seven_plastics/6_polystyrene_PS", polystyrene_PS_files, []float64{0, 0, 0, 0, 0, 1, 0, 0}}

	other_resins_files, err := ioutil.ReadDir("./seven_plastics/7_other_resins")
	handelError(err)
	other_resins := objects{"./seven_plastics/7_other_resins", other_resins_files, []float64{0, 0, 0, 0, 0, 0, 1, 0}}

	no_plastic_files, err := ioutil.ReadDir("./seven_plastics/8_no_plastic")
	handelError(err)
	no_plastic := objects{"./seven_plastics/8_no_plastic", no_plastic_files, []float64{0, 0, 0, 0, 0, 0, 0, 1}}

	list := []objects{
		polyethylene_PET,
		high_density_polyethylene_PEHD,
		polyvinylchloride_PVC,
		low_density_polyethylene_PELD,
		polypropylene_PP,
		polystyrene_PS,
		other_resins,
		no_plastic,
	}

	f := make([]files, 0)
	for _, v := range list {
		for _, vf := range v.file {
			f = append(f, files{v.path + "/" + vf.Name(), v.outputs})
		}
	}

	shuffle(&f)

	aa := time.Now()
	fmt.Println("Creating Neural Network")
	var nn = new(neuraln.NeuralN)
	nn.Create(2000, 200, 8)
	fmt.Println(time.Since(aa), "\n")
	fmt.Println("Training the Neural-N")
	for i := 0; i < 300; i++ {
		shuffle(&f)
		fmt.Println(i + 1)
		for _, v := range f {
			data := iprocessing.SetUpTrainingData(v.fName, 200, 200)
			nn.Train(*data, v.outputs)
		}
	}
	fmt.Println(time.Since(aa), "\n")

	fmt.Println("Saving the Neural")
	jsonData, err := nn.ExportJSON()
	handelError(err)
	fs, err := os.Create("trained-Data.json")
	handelError(err)
	fs.Write(jsonData)
	fmt.Println(time.Since(aa), "\n")

	// data, err := ioutil.ReadFile("/data/data/com.lilHesham.ecocycle/binaries/trained-Data.json")
	// handelError(err)
	// nn, err := neuraln.ImportJSON(data)
	// handelError(err)

	// plasticList := []string{
	// 	"polyethylene_PET",
	// 	"high_density_polyethylene_PEHD",
	// 	"polyvinylchloride_PVC",
	// 	"low_density_polyethylene_PELD",
	// 	"polypropylene_PP",
	// 	"polystyrene_PS",
	// 	"other_resins",
	// 	"not a plastic",
	// }

	// ln := os.Args[1]
	// imgdata := iprocessing.SetUpTrainingData(ln, 200, 200)
	// output := nn.FeedForword(*imgdata)
	// outData := make([]float64, 0)

	// for _, v := range output.Matrix {
	// 	outData = append(outData, v[0])
	// }

	// plastics := make([]string, 0)
	// sort.Float64s(outData)

	// for i := 7; i >= 5; i-- {
	// 	for j, outputV := range output.Matrix {
	// 		if outData[i] == outputV[0] {
	// 			plastics = append(plastics, plasticList[j])
	// 		}
	// 	}
	// }

	// for _, v := range output.Matrix {
	// 	if v[0] == outData[len(outData)-1] {
	// 		fmt.Printf("%0.18f,%s,%0.18f,%s,%0.18f,%s",
	// 			outData[7], plastics[0], outData[6], plastics[1], outData[5], plastics[2])
	// 		break
	// 	}
	// }

}

func handelError(err error) {
	if err != nil {
		log.Fatal(err)
	}
}
