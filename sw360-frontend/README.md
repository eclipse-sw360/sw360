# Sw360-frontend

This is the POC of Sw360 frontend with ReactJS

Implemented functions:
 + Login page


# Environment

- **Node** version: v16.19.0
- **Npm** version: 9.4.1

- Install commands:

```
    curl -sL https://deb.nodesource.com/setup_16.x | sudo -E bash -
    sudo apt install nodejs -y  
```

# Project structure

```
├── public                                  // public resources
│   ├── favicon
│   └── images
└── src
    ├── assets
    │   ├── icons
    │   └── images
    ├── contexts                            // define contexts
    ├── object-types                        // define object type
    ├── routing                             // define route configuration
    │   └── routes                          // define sub routes
    |   └── route.configuration.tsx         // main route config file
    ├── services                            // service files
    ├── utils                               // util files
    └── views                               // page views
├── .env                                    // environment file

```

# How to run ?

+ Create **.env** file in folder sw360-frontend to config rest api url

```
REACT_APP_SW360_API_URL = ${SW360_API_URL}   (e.g 'http://localhost:8080')
```

+ Config SW360 Rest API CORS to allow the requests from ReactJS frontend (http://localhost:3000)

+ At sw360-frontend folder, run command to start (after sw360 Rest API is started):

```
npm start
```

### Your frontend will be running in port 3000


