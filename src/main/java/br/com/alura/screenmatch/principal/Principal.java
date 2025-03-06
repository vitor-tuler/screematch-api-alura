package br.com.alura.screenmatch.principal;

import br.com.alura.screenmatch.model.*;
import br.com.alura.screenmatch.repository.SerieRepository;
import br.com.alura.screenmatch.service.ConsumoApi;
import br.com.alura.screenmatch.service.ConverteDados;

import java.util.*;
import java.util.stream.Collectors;

public class Principal {

    private Scanner leitura = new Scanner(System.in);
    private ConsumoApi consumo = new ConsumoApi();
    private ConverteDados conversor = new ConverteDados();
    private final String ENDERECO = "https://www.omdbapi.com/?t=";
    private final String API_KEY = "&apikey=6585022c";
    private List<DadosSerie> dadosSeries = new ArrayList<>();

    private SerieRepository repository;

    private List<Serie> series = new ArrayList<>();

    private Optional<Serie> serieBusca;

    public Principal(SerieRepository repository) {
        this.repository = repository;
    }

    public void exibeMenu() {
        var opcao = -1;
        while(opcao != 0) {
            var menu = """
                    1 - Buscar séries
                    2 - Buscar episódios
                    3 - Listar séries buscadas
                    4 - Buscar séries por título
                    5 - Buscar séries por ator
                    6 - Top 5 séries
                    7 - Buscar series por categoria
                    8 - Filtrar series
                    9 - Buscar episodios por trecho
                    10 - Top 5 episodios por serie
                    11 - Buscar episodios a partir de uma data
                                    
                    0 - Sair                                 
                    """;

            System.out.println(menu);
            opcao = leitura.nextInt();
            leitura.nextLine();

            switch (opcao) {
                case 1:
                    buscarSerieWeb();
                    break;
                case 2:
                    buscarEpisodioPorSerie();
                    break;
                case 3:
                    listarSeriesBuscadas();
                    break;
                case 4:
                    buscarSeriePorTitulo();
                    break;
                case 5:
                    buscarSeriePorAtor();
                    break;
                case 6:
                    buscarTop5Series();
                    break;
                case 7:
                    buscarTopSeriesPorCategoria();
                    break;
                case 8:
                    filtrarSeriesPorTemporadasEAvaliacao();
                    break;
                case 9:
                    buscarEpisodioPorTrecho();
                    break;
                case 10:
                    topEpisodiosPorSerie();
                    break;
                case 11:
                    buscarEpisodiosAPartirDeUmaData();
                    break;
                case 0:
                    System.out.println("Saindo...");
                    break;
                default:
                    System.out.println("Opção inválida");
            }
        }
    }

    private void buscarSerieWeb() {
        DadosSerie dados = getDadosSerie();
        Serie serie = new Serie(dados);
        repository.save(serie);
        System.out.println(dados);
    }

    private DadosSerie getDadosSerie() {
        System.out.println("Digite o nome da série para busca");
        var nomeSerie = leitura.nextLine();
        var json = consumo.obterDados(ENDERECO + nomeSerie.replace(" ", "+") + API_KEY);
        DadosSerie dados = conversor.obterDados(json, DadosSerie.class);
        return dados;
    }

    private void buscarEpisodioPorSerie(){
        listarSeriesBuscadas();
        System.out.println("Escolha uma serie pelo nome:");
        var nomeSerie = leitura.nextLine();
        Optional<Serie> serie = series.stream()
                .filter(s -> s.getTitulo().toLowerCase().contains(nomeSerie.toLowerCase()))
                .findFirst();

        if(serie.isPresent()) {

            var serieEncontrada = serie.get();
            List<DadosTemporada> temporadas = new ArrayList<>();

            for (int i = 1; i <= serieEncontrada.getTotalTemporadas(); i++) {
                var json = consumo.obterDados(ENDERECO + serieEncontrada.getTitulo().replace(" ", "+") + "&season=" + i + API_KEY);
                DadosTemporada dadosTemporada = conversor.obterDados(json, DadosTemporada.class);
                temporadas.add(dadosTemporada);
            }
            temporadas.forEach(System.out::println);

            List<Episodio> episodios = temporadas.stream()
                    .flatMap(d -> d.episodios().stream()
                            .map(e -> new Episodio(d.numero(), e)))
                    .collect(Collectors.toList());
            serieEncontrada.setEpisodios(episodios);
            repository.save(serieEncontrada);
        }else{
            System.out.println("Serie nao encontrada");
        }
    }

    private void listarSeriesBuscadas(){
        series  = repository.findAll();
        series.stream()
                .sorted(Comparator.comparing(Serie::getGenero))
                .forEach(System.out::println);
    }

    private void buscarSeriePorTitulo() {
        System.out.println("Escolha uma serie pelo nome:");
        var nomeSerie = leitura.nextLine();
        serieBusca = repository.findByTituloContainingIgnoreCase(nomeSerie);

        if(serieBusca.isPresent()){
            System.out.println("Dados da serie: " + serieBusca.get());
        } else{
            System.out.println("serie nao encontrada");
        }
    }

    private void buscarSeriePorAtor() {
        System.out.println("Qual o nome para busca? ");
        var nomeAtor = leitura.nextLine();
        List<Serie> seriesEncontradas = repository.findByAtoresContainingIgnoreCase(nomeAtor);
        System.out.println("Series com " + nomeAtor);
        seriesEncontradas.forEach(serie ->
                System.out.println(serie.getTitulo() + " avaliacao: " + serie.getAvaliacao()));
    }

    private void buscarTop5Series() {
        List<Serie> serieTop = repository.findTop5ByOrderByAvaliacaoDesc();
        serieTop.forEach(s ->
                System.out.println(s.getTitulo() + " avaliacao: " + s.getAvaliacao()));
    }

    private void buscarTopSeriesPorCategoria() {
        System.out.println("Categoria a ser buscada: ");
        var nomeGenero = leitura.nextLine();
        Categoria categoria = Categoria.fromPortugues(nomeGenero);
        List<Serie> seriesPorCategoria = repository.findByGenero(categoria);
        System.out.println("Series do genero" + nomeGenero);
        seriesPorCategoria.forEach(System.out::println);
    }

    private void filtrarSeriesPorTemporadasEAvaliacao() {
        System.out.println("filtrar series por quantas temporadas");
        var totalTemporadas = leitura.nextInt();
        leitura.nextLine();
        System.out.println("A partir de qual avaliacao? ");
        var avaliacao = leitura.nextDouble();
        leitura.nextLine();
        List<Serie> filtroSerie = repository.seriesPorTemporadaEAvaliacao(totalTemporadas, avaliacao);
        System.out.println("*** Series filtradas ***");
        filtroSerie.forEach(s ->
                System.out.println(s.getTitulo() + " - avaliacao: " + s.getAvaliacao()));
    }

    private void buscarEpisodioPorTrecho() {
        System.out.println("Qual o nome do episodio para a busca? ");
        var trechoEpisodio = leitura.nextLine();
        List<Episodio> episodiosEncontrados = repository.episodiosPorTrecho(trechoEpisodio);
        episodiosEncontrados.forEach(e->
                System.out.printf("serie: %s Temporada %s - Episodio %s - %s\n",
                        e.getSerie().getTitulo(), e.getTemporada(),
                        e.getNumeroEpisodio(), e.getTitulo()));
    }

    private void topEpisodiosPorSerie(){
        buscarSeriePorTitulo();
        if(serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            List<Episodio> topEpisodios = repository.topEpisodiosPorSerie(serie);
            topEpisodios.forEach(e ->
                    System.out.printf("Série: %s Temporada %s - Episódio %s - %s Avaliação %s\n",
                            e.getSerie().getTitulo(), e.getTemporada(),
                            e.getNumeroEpisodio(), e.getTitulo(), e.getAvaliacao() ));
        }
    }

    private void buscarEpisodiosAPartirDeUmaData(){
        buscarSeriePorTitulo();
        if (serieBusca.isPresent()){
            Serie serie = serieBusca.get();
            System.out.println("Digite o ano limite de lancamento");
            var anoLancamento = leitura.nextInt();
            leitura.nextLine();

            List<Episodio> episodioAno = repository.episodiosPorTrechoEAno(serie, anoLancamento);
            episodioAno.forEach(System.out::println);
        }
    }
}
