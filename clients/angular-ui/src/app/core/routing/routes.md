This documents describes the SW360 client routes.

## General
/                                   home
/user                               user profile & settings

### Resources

All self-contained entities have a one segment (list/table) route:

- /components                         list of components (ROOT)
- /projects                           list of projects (ROOT)
- /licenses                           list of licenses (ROOT)
- ...

A special case is releases, it is only available as embedded list of resources:

- /components/:id/releases

### Resource Details

All self-contained entities have a two segment (detail) route:

- /components/:id
- /projects/:id
- /licenses/:id
- ...

A special case is release, it is only available as embedded detail:

- /components/:id/releases/:id
    
The other special case is projects that can have other linked projects:

- /projects/:id/projects

In the case of opening the detail of an embedded project we reroute to the two segment (detail) route as described above:

- /projects/:id

### Resource Mutation

All self-contained entities have a three segment (edit) route:

- /components/:id/edit
- /projects/:id/edit
- /licenses/:id/edit
- ...

The edit route is used for creating as well as editing an existing resource. The route event handles the initialization of the activated component.

As stated release gets treated as a specification of component, this also applies for editing its data:

- /components/:id/releases/:id/edit

### Router logic

The implementation of the router is derived from above described route definitions:

- If the last route segment is a resource type (components, projects, licenses, releases) the view must be a list of resources that is specified by the resource type.
- If the last route segment is not known and the next to last (penultimate) segment is a resource type the view must be a detail.
- If the last route segment is `edit` it must be a edit view of the next to last (penultimate) resource type.
- If there is no resource type in the route it must be a specific general route.
