# Como executar o projeto

Abrir um terminal na raiz do projeto e executar os seguintes comandos

## Preparar o Frontend (React)
```bash
cd frontEnd
npm install
npm run build
```

## Preparar o Backend (Spring Boot & Docker Images)
```bash
cd .. 
.\gradlew :http:buildImageAll
```
**O comando tem que ser executado na raiz do projeto**

## Iniciar os Contentores (Docker Compose)
```bash
cd docker
docker compose up -d --build
```


O frontend vai estar disponivel em: http://localhost:3000/

